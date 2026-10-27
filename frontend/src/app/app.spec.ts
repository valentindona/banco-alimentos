import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, vi } from 'vitest';

describe('App', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    sessionStorage.removeItem('bda-token');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  it('creates the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('renders the login screen', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Mar del Plata');
  });

  it('shows only the modules authorized for the current role', () => {
    const app = TestBed.createComponent(App).componentInstance;
    app.user.set({
      usuarioId: 1,
      nombreUsuario: 'tesoreria',
      rol: 'USER_TESORERIA',
      institucionId: null,
    });
    expect(app.visibleModules.map((module) => module.key)).toEqual(['instituciones']);
  });

  it('rejects opening a module not assigned to the current role', async () => {
    const app = TestBed.createComponent(App).componentInstance;
    app.user.set({
      usuarioId: 2,
      nombreUsuario: 'institucional',
      rol: 'USER_INSTITUCIONAL',
      institucionId: 10,
    });
    await app.openModule({
      key: 'productos',
      label: 'Productos',
      description: '',
      endpoint: '/api/productos',
      icon: 'PR',
      roles: ['USER_DEPOSITO'],
    });
    expect(app.activeModule()).toBeNull();
    expect(app.error()).toContain('no tiene permisos');
  });

  it('lists pages and exposes create, edit and delete through the product screen', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.user.set({
      usuarioId: 1,
      nombreUsuario: 'deposito',
      rol: 'USER_DEPOSITO',
      institucionId: null,
    });
    const productos = {
      key: 'productos' as const,
      label: 'Productos',
      description: '',
      endpoint: '/api/productos',
      icon: 'PR',
      roles: ['USER_DEPOSITO' as const],
    };

    const opened = app.openModule(productos);
    const firstPage = httpMock.expectOne('/api/productos?pagina=0&tamano=20');
    firstPage.flush({
      contenido: [{ id: 21, nombre: 'Arroz', descripcion: '', tipoAlimento: 'Seco', unidadMedida: 'kg' }],
      pagina: 0,
      totalPaginas: 2,
    });
    await opened;
    expect(app.rows).toHaveLength(1);
    expect(app.totalPages()).toBe(2);

    app.openCreateForm();
    app.formValues.set({
      nombre: 'Harina de prueba',
      descripcion: '',
      tipoAlimento: 'Seco',
      unidadMedida: 'kg',
    });
    const create = app.submitForm();
    httpMock.expectOne('/api/productos').flush({ id: 22 });
    await Promise.resolve();
    const afterCreate = httpMock.expectOne('/api/productos?pagina=0&tamano=20');
    afterCreate.flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await create;

    app.openEditForm({
      id: 21,
      nombre: 'Arroz',
      descripcion: '',
      tipoAlimento: 'Seco',
      unidadMedida: 'kg',
    });
    app.updateFormValue('nombre', 'Arroz integral');
    const update = app.submitForm();
    httpMock.expectOne('/api/productos/21').flush({ id: 21 });
    await Promise.resolve();
    httpMock.expectOne('/api/productos?pagina=0&tamano=20')
      .flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await update;

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const remove = app.deactivate({ id: 21, nombre: 'Arroz integral' });
    httpMock.expectOne('/api/productos/21').flush(null);
    await Promise.resolve();
    httpMock.expectOne('/api/productos?pagina=0&tamano=20')
      .flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await remove;
  });

  it('uses named reference selectors and allows draft remito/adjustment edits', () => {
    const app = TestBed.createComponent(App).componentInstance;
    app.user.set({
      usuarioId: 1,
      nombreUsuario: 'deposito',
      rol: 'USER_DEPOSITO',
      institucionId: null,
    });
    app.activeModule.set({
      key: 'ajustes',
      label: 'Ajustes',
      description: '',
      endpoint: '/api/ajustes',
      icon: 'AJ',
      roles: ['USER_DEPOSITO'],
    });
    app.formMode.set('create');
    expect(app.formFields.find((field) => field.name === 'stockId')?.type).toBe('select');

    app.formValues.set({ tipo: 'CORRECCION' });
    expect(app.minimumValue({ name: 'cantidad', label: 'Cantidad', type: 'number' })).toBe('0');
    const adjustment = {
      id: 3,
      stockId: 7,
      cantidadAnterior: 10,
      cantidadNueva: 8,
      tipo: 'BAJA',
      motivo: 'Ajuste',
    };
    expect(app.canEditRow(adjustment)).toBe(true);
    expect(app.canDeactivateRow(adjustment)).toBe(true);

    app.activeModule.set({
      key: 'remitos',
      label: 'Remitos',
      description: '',
      endpoint: '/api/remitos',
      icon: 'RE',
      roles: ['USER_DEPOSITO'],
    });
    expect(
      app.showFormField({ name: 'institucionId', label: 'Institución', type: 'select' }),
    ).toBe(true);
    expect(app.canManageRemito({ id: 8, estado: 'BORRADOR' })).toBe(true);
    expect(app.canEditRow({ id: 8, estado: 'BORRADOR' })).toBe(true);
    expect(app.canEditRow({ id: 8, estado: 'CONFIRMADO' })).toBe(false);
  });

  it('creates, edits and deletes adjustments from the stock adjustment screen', async () => {
    const app = TestBed.createComponent(App).componentInstance;
    app.user.set({
      usuarioId: 1,
      nombreUsuario: 'deposito',
      rol: 'USER_DEPOSITO',
      institucionId: null,
    });
    app.activeModule.set({
      key: 'ajustes',
      label: 'Ajustes de stock',
      description: '',
      endpoint: '/api/ajustes',
      icon: 'AJ',
      roles: ['USER_DEPOSITO'],
    });

    app.formMode.set('create');
    app.formValues.set({ stockId: '7', tipo: 'BAJA', cantidad: '2.5', motivo: 'Merma' });
    const create = app.submitForm();
    const createRequest = httpMock.expectOne('/api/ajustes');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual({
      stockId: 7,
      tipo: 'BAJA',
      cantidad: 2.5,
      motivo: 'Merma',
    });
    createRequest.flush({ id: 9 });
    await Promise.resolve();
    httpMock.expectOne('/api/ajustes?pagina=0&tamano=20')
      .flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await create;

    app.formMode.set('edit');
    app.editingRow.set({ id: 9, stockId: 7 });
    app.formValues.set({ stockId: '7', tipo: 'CORRECCION', cantidad: '0', motivo: 'Conteo real' });
    const update = app.submitForm();
    const updateRequest = httpMock.expectOne('/api/ajustes/9');
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual({
      stockId: 7,
      tipo: 'CORRECCION',
      cantidad: 0,
      motivo: 'Conteo real',
    });
    updateRequest.flush({ id: 9 });
    await Promise.resolve();
    httpMock.expectOne('/api/ajustes?pagina=0&tamano=20')
      .flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await update;

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const remove = app.deactivate({ id: 9 });
    const removeRequest = httpMock.expectOne('/api/ajustes/9');
    expect(removeRequest.request.method).toBe('DELETE');
    removeRequest.flush(null);
    await Promise.resolve();
    httpMock.expectOne('/api/ajustes?pagina=0&tamano=20')
      .flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await remove;
  });

  it('edits a draft remito and its detail through their visible actions', async () => {
    const app = TestBed.createComponent(App).componentInstance;
    app.user.set({
      usuarioId: 1,
      nombreUsuario: 'deposito',
      rol: 'USER_DEPOSITO',
      institucionId: null,
    });
    app.activeModule.set({
      key: 'remitos',
      label: 'Remitos',
      description: '',
      endpoint: '/api/remitos',
      icon: 'RE',
      roles: ['USER_DEPOSITO'],
    });
    app.formMode.set('edit');
    app.editingRow.set({ id: 14, estado: 'BORRADOR' });
    app.formValues.set({ institucionId: '1', fechaEntrega: '2026-11-02' });

    const updateRemito = app.submitForm();
    const remitoRequest = httpMock.expectOne('/api/remitos/14');
    expect(remitoRequest.request.method).toBe('PUT');
    expect(remitoRequest.request.body).toEqual({ institucionId: 1, fechaEntrega: '2026-11-02' });
    remitoRequest.flush({ id: 14 });
    await Promise.resolve();
    httpMock.expectOne('/api/remitos?pagina=0&tamano=20')
      .flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await updateRemito;

    app.formMode.set('remito-detail');
    app.editingRow.set({ id: 14, estado: 'BORRADOR' });
    app.editingDetail.set({ id: 6 });
    app.formValues.set({ stockId: '7', cantidad: '1.5' });
    const updateDetail = app.submitForm();
    const detailRequest = httpMock.expectOne('/api/remitos/14/detalles/6');
    expect(detailRequest.request.method).toBe('PUT');
    expect(detailRequest.request.body).toEqual({ stockId: 7, cantidad: 1.5 });
    detailRequest.flush({ id: 6 });
    await Promise.resolve();
    httpMock.expectOne('/api/remitos?pagina=0&tamano=20')
      .flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await updateDetail;
  });

  it('generates the missing invoice from the donation screen', async () => {
    const app = TestBed.createComponent(App).componentInstance;
    app.user.set({
      usuarioId: 1,
      nombreUsuario: 'deposito',
      rol: 'USER_DEPOSITO',
      institucionId: null,
    });
    app.activeModule.set({
      key: 'donaciones',
      label: 'Donaciones',
      description: '',
      endpoint: '/api/donaciones',
      icon: 'DN',
      roles: ['USER_DEPOSITO'],
    });

    const generate = app.generateInvoice({ id: 32, comprobante: null });
    const request = httpMock.expectOne('/api/facturas/generacion');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ donacionId: 32 });
    request.flush({ id: 15 });
    await Promise.resolve();
    httpMock.expectOne('/api/donaciones?pagina=0&tamano=20')
      .flush({ contenido: [], pagina: 0, totalPaginas: 1 });
    await generate;
  });
});
