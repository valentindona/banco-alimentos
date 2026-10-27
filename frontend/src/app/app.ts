import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

type Role = 'USER_DEPOSITO' | 'USER_INSTITUCIONAL' | 'USER_TESORERIA';
type ModuleKey =
  | 'productos'
  | 'donantes'
  | 'stock'
  | 'donaciones'
  | 'remitos'
  | 'ajustes'
  | 'facturas'
  | 'instituciones'
  | 'notificaciones'
  | 'usuarios';
type FormMode = 'create' | 'edit' | 'payment' | 'remito-detail';

interface User {
  usuarioId: number;
  nombreUsuario: string;
  rol: Role;
  institucionId: number | null;
}

interface ModuleLink {
  key: ModuleKey;
  label: string;
  description: string;
  endpoint: string;
  icon: string;
  roles: Role[];
}

interface FormField {
  name: string;
  label: string;
  type: 'text' | 'email' | 'password' | 'number' | 'date' | 'select' | 'textarea';
  required?: boolean;
  options?: string[];
  hint?: string;
}

interface DonationItem {
  productoId: string;
  cantidad: string;
  fechaVencimiento: string;
}

type DataRow = Record<string, unknown>;

const D: Role = 'USER_DEPOSITO';
const I: Role = 'USER_INSTITUCIONAL';
const T: Role = 'USER_TESORERIA';

const MODULES: ModuleLink[] = [
  {
    key: 'productos',
    label: 'Productos',
    description: 'Catálogo y datos de alimentos',
    endpoint: '/api/productos',
    icon: 'PR',
    roles: [D],
  },
  {
    key: 'donantes',
    label: 'Donantes',
    description: 'Contactos que realizan donaciones',
    endpoint: '/api/donantes',
    icon: 'DO',
    roles: [D],
  },
  {
    key: 'stock',
    label: 'Stock',
    description: 'Lotes disponibles y vencimientos',
    endpoint: '/api/stock',
    icon: 'ST',
    roles: [D],
  },
  {
    key: 'donaciones',
    label: 'Donaciones',
    description: 'Recepciones y comprobantes',
    endpoint: '/api/donaciones',
    icon: 'DN',
    roles: [D],
  },
  {
    key: 'remitos',
    label: 'Remitos',
    description: 'Preparación y entrega a instituciones',
    endpoint: '/api/remitos',
    icon: 'RE',
    roles: [D],
  },
  {
    key: 'ajustes',
    label: 'Ajustes de stock',
    description: 'Correcciones, bajas e historial',
    endpoint: '/api/ajustes',
    icon: 'AJ',
    roles: [D],
  },
  {
    key: 'facturas',
    label: 'Facturas',
    description: 'Consulta de facturación',
    endpoint: '/api/facturas',
    icon: 'FA',
    roles: [D],
  },
  {
    key: 'instituciones',
    label: 'Instituciones',
    description: 'Organizaciones y estado de pago',
    endpoint: '/api/instituciones',
    icon: 'IN',
    roles: [D, I, T],
  },
  {
    key: 'notificaciones',
    label: 'Notificaciones',
    description: 'Avisos operativos',
    endpoint: '/api/notificaciones',
    icon: 'NO',
    roles: [D],
  },
  {
    key: 'usuarios',
    label: 'Usuarios',
    description: 'Alta de usuarios del sistema',
    endpoint: '/api/usuarios',
    icon: 'US',
    roles: [D],
  },
];

const MODULE_BY_KEY = new Map(MODULES.map((module) => [module.key, module]));

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly http = inject(HttpClient);
  readonly user = signal<User | null>(null);
  readonly activeModule = signal<ModuleLink | null>(null);
  readonly payload = signal<unknown>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly formMode = signal<FormMode | null>(null);
  readonly formValues = signal<Record<string, string>>({});
  readonly editingRow = signal<DataRow | null>(null);
  readonly pageIndex = signal(0);
  readonly totalPages = signal(0);
  readonly donationItems = signal<DonationItem[]>([]);
  username = '';
  password = '';

  constructor() {
    const token = sessionStorage.getItem('bda-token');
    if (token) void this.loadIdentity(token);
  }

  get visibleModules(): ModuleLink[] {
    const role = this.user()?.rol;
    return role ? MODULES.filter((module) => module.roles.includes(role)) : [];
  }

  get rows(): DataRow[] {
    const result = this.payload();
    if (Array.isArray(result)) return result.filter(this.isDataRow);
    if (!this.isDataRow(result)) return [];
    const content = result['contenido'] ?? result['content'];
    return Array.isArray(content) ? content.filter(this.isDataRow) : [];
  }

  get formFields(): FormField[] {
    const module = this.activeModule()?.key;
    const mode = this.formMode();
    if (!module || !mode) return [];

    if (module === 'productos') {
      return [
        { name: 'nombre', label: 'Nombre', type: 'text', required: true },
        { name: 'descripcion', label: 'Descripción', type: 'textarea' },
        { name: 'tipoAlimento', label: 'Tipo de alimento', type: 'text', required: true },
        {
          name: 'unidadMedida',
          label: 'Unidad de medida',
          type: 'text',
          required: true,
          hint: 'Por ejemplo: kg, litros o unidades.',
        },
      ];
    }
    if (module === 'donantes') {
      return [
        { name: 'nombre', label: 'Nombre', type: 'text', required: true },
        { name: 'correo', label: 'Correo electrónico', type: 'email' },
        { name: 'telefono', label: 'Teléfono', type: 'text' },
      ];
    }
    if (module === 'instituciones') {
      if (mode === 'payment') {
        return [
          {
            name: 'estadoPago',
            label: 'Estado de pago',
            type: 'select',
            required: true,
            options: ['AL_DIA', 'EN_DEUDA', 'PARTICULAR'],
          },
        ];
      }
      return [
        { name: 'nombre', label: 'Nombre', type: 'text', required: true },
        { name: 'domicilio', label: 'Domicilio', type: 'text', required: true },
        { name: 'tipo', label: 'Tipo', type: 'text', required: true },
      ];
    }
    if (module === 'usuarios' && mode === 'create') {
      return [
        { name: 'nombreUsuario', label: 'Nombre de usuario', type: 'text', required: true },
        {
          name: 'clave',
          label: 'Contraseña',
          type: 'password',
          required: true,
          hint: 'Debe tener entre 12 y 128 caracteres.',
        },
        {
          name: 'rol',
          label: 'Rol',
          type: 'select',
          required: true,
          options: ['USER_DEPOSITO', 'USER_INSTITUCIONAL', 'USER_TESORERIA'],
        },
        {
          name: 'institucionId',
          label: 'ID de institución',
          type: 'number',
          hint: 'Obligatorio solo para el rol institucional.',
        },
      ];
    }
    if (module === 'donaciones' && mode === 'create') {
      return [
        { name: 'donanteId', label: 'ID del donante', type: 'number', required: true },
        { name: 'fecha', label: 'Fecha de recepción', type: 'date', required: true },
      ];
    }
    if (module === 'remitos') {
      if (mode === 'create') {
        return [
          { name: 'institucionId', label: 'ID de institución', type: 'number', required: true },
          { name: 'fechaEntrega', label: 'Fecha de entrega', type: 'date', required: true },
        ];
      }
      if (mode === 'remito-detail') {
        return [
          { name: 'stockId', label: 'ID del lote de stock', type: 'number', required: true },
          { name: 'cantidad', label: 'Cantidad', type: 'number', required: true },
        ];
      }
    }
    if (module === 'ajustes' && mode === 'create') {
      return [
        { name: 'stockId', label: 'ID del lote de stock', type: 'number', required: true },
        {
          name: 'tipo',
          label: 'Tipo de ajuste',
          type: 'select',
          required: true,
          options: ['CORRECCION', 'BAJA'],
        },
        { name: 'cantidad', label: 'Cantidad', type: 'number', required: true },
        { name: 'motivo', label: 'Motivo', type: 'text', required: true },
      ];
    }
    return [];
  }

  get formTitle(): string {
    const module = this.activeModule()?.label ?? 'Registro';
    const mode = this.formMode();
    if (mode === 'edit') return `Editar ${module.toLocaleLowerCase('es-AR')}`;
    if (mode === 'payment') return 'Actualizar estado de pago';
    if (mode === 'remito-detail') return 'Agregar producto al remito';
    return `Agregar ${module.toLocaleLowerCase('es-AR')}`;
  }

  get canCreate(): boolean {
    const role = this.user()?.rol;
    const key = this.activeModule()?.key;
    return (
      ((key === 'productos' ||
        key === 'donantes' ||
        key === 'donaciones' ||
        key === 'remitos' ||
        key === 'ajustes') &&
        role === D) ||
      (key === 'instituciones' && role === I) ||
      (key === 'usuarios' && role === D)
    );
  }

  showFormField(field: FormField): boolean {
    return field.name !== 'institucionId' || this.formValues()['rol'] === I;
  }

  async login(): Promise<void> {
    this.error.set('');
    this.busy.set(true);
    try {
      const response = await firstValueFrom(
        this.http.post<{ token: string }>('/api/auth/login', {
          nombreUsuario: this.username.trim(),
          clave: this.password,
        }),
      );
      sessionStorage.setItem('bda-token', response.token);
      this.password = '';
      await this.loadIdentity(response.token);
    } catch (error) {
      this.showError(error, 'No se pudo iniciar sesión. Revisá las credenciales.');
    } finally {
      this.busy.set(false);
    }
  }

  async openModule(module: ModuleLink): Promise<void> {
    const permittedModule = MODULE_BY_KEY.get(module.key);
    if (
      !permittedModule ||
      !this.visibleModules.some((visible) => visible.key === permittedModule.key)
    ) {
      this.error.set('Tu usuario no tiene permisos para acceder a este módulo.');
      return;
    }
    this.activeModule.set(permittedModule);
    this.closeForm();
    if (permittedModule.key === 'usuarios') {
      this.error.set('');
      this.payload.set([]);
      this.pageIndex.set(0);
      this.totalPages.set(0);
      return;
    }
    await this.loadModule(0);
  }

  async changePage(page: number): Promise<void> {
    if (page < 0 || page >= this.totalPages()) return;
    await this.loadModule(page);
  }

  openCreateForm(): void {
    if (!this.canCreate) return;
    this.beginForm('create', null, this.defaultFormValues());
    if (this.activeModule()?.key === 'donaciones') {
      this.donationItems.set([
        { productoId: '', cantidad: '', fechaVencimiento: this.localDate() },
      ]);
    }
  }

  openEditForm(row: DataRow): void {
    const module = this.activeModule()?.key;
    const role = this.user()?.rol;
    if (!this.rowId(row)) return;
    if (
      !((module === 'productos' || module === 'donantes') && role === D) &&
      !(module === 'instituciones' && role === I)
    )
      return;
    this.beginForm('edit', row, this.valuesFromRow(row));
  }

  openPaymentForm(row: DataRow): void {
    if (this.activeModule()?.key !== 'instituciones' || this.user()?.rol !== T || !this.rowId(row))
      return;
    this.beginForm('payment', row, { estadoPago: this.rowText(row, 'estadoPago') });
  }

  openRemitoDetailForm(row: DataRow): void {
    if (this.activeModule()?.key !== 'remitos' || this.user()?.rol !== D || !this.rowId(row))
      return;
    this.beginForm('remito-detail', row, {});
  }

  closeForm(): void {
    this.formMode.set(null);
    this.formValues.set({});
    this.editingRow.set(null);
    this.donationItems.set([]);
  }

  updateFormValue(name: string, value: string): void {
    this.formValues.update((values) => ({ ...values, [name]: value }));
  }

  updateDonationItem(index: number, field: keyof DonationItem, value: string): void {
    this.donationItems.update((items) =>
      items.map((item, itemIndex) => (itemIndex === index ? { ...item, [field]: value } : item)),
    );
  }

  addDonationItem(): void {
    this.donationItems.update((items) => [
      ...items,
      {
        productoId: '',
        cantidad: '',
        fechaVencimiento: this.localDate(),
      },
    ]);
  }

  removeDonationItem(index: number): void {
    this.donationItems.update((items) => items.filter((_, itemIndex) => itemIndex !== index));
  }

  async submitForm(): Promise<void> {
    const module = this.activeModule();
    const mode = this.formMode();
    if (!module || !mode || !this.formFields.length) return;
    this.error.set('');
    this.busy.set(true);
    try {
      const values = this.formValues();
      const rowId = this.rowId(this.editingRow());
      const url =
        mode === 'edit' && rowId
          ? `${module.endpoint}/${rowId}`
          : mode === 'payment' && rowId
            ? `${module.endpoint}/${rowId}/estado-pago`
            : mode === 'remito-detail' && rowId
              ? `${module.endpoint}/${rowId}/detalles`
              : module.endpoint;
      const method = mode === 'edit' ? 'put' : mode === 'payment' ? 'patch' : 'post';
      const body = this.formPayload(module.key, mode, values);
      const request =
        method === 'put'
          ? this.http.put<unknown>(url, body, this.requestOptions())
          : method === 'patch'
            ? this.http.patch<unknown>(url, body, this.requestOptions())
            : this.http.post<unknown>(url, body, this.requestOptions());
      await firstValueFrom(request);
      this.closeForm();
      await this.loadModule(this.pageIndex());
    } catch (error) {
      this.showError(error, 'No se pudo guardar. Revisá los datos e intentá nuevamente.');
    } finally {
      this.busy.set(false);
    }
  }

  async confirmRemito(row: DataRow): Promise<void> {
    const id = this.rowId(row);
    if (this.activeModule()?.key !== 'remitos' || this.user()?.rol !== D || !id) return;
    if (!window.confirm(`¿Confirmar el remito #${id}? Esta acción no se puede deshacer.`)) return;
    await this.performAction(
      `${MODULE_BY_KEY.get('remitos')?.endpoint}/${id}/confirmacion`,
      'post',
      {},
      'No se pudo confirmar el remito.',
    );
  }

  async deactivate(row: DataRow): Promise<void> {
    const module = this.activeModule();
    const id = this.rowId(row);
    const key = module?.key;
    const allowed =
      (this.user()?.rol === D && (key === 'productos' || key === 'donantes')) ||
      (this.user()?.rol === I && key === 'instituciones');
    if (!module || !id || !allowed) return;
    if (!window.confirm(`¿Dar de baja este registro?`)) return;
    await this.performAction(
      `${module.endpoint}/${id}`,
      'delete',
      null,
      'No se pudo dar de baja el registro.',
    );
  }

  cardTitle(row: DataRow): string {
    return this.firstText(
      row,
      ['nombre', 'productoNombre', 'donanteNombre', 'institucionNombre', 'tipo', 'mensaje'],
      'Registro',
    );
  }

  cardSubtitle(row: DataRow): string {
    const id = this.rowId(row);
    return id
      ? `Registro #${id}`
      : this.firstText(row, ['fecha', 'fechaCreacion', 'fechaHora', 'fechaEntrega'], '');
  }

  cardDetails(row: DataRow): string[] {
    const excluded = new Set([
      'id',
      'nombre',
      'productoNombre',
      'donanteNombre',
      'institucionNombre',
      'items',
      'detalles',
      'comprobante',
    ]);
    return Object.entries(row)
      .filter(
        ([key, value]) =>
          !excluded.has(key) && value !== null && value !== undefined && typeof value !== 'object',
      )
      .slice(0, 4)
      .map(([key, value]) => `${this.humanize(key)}: ${String(value)}`);
  }

  rowId(row: DataRow | null): string {
    const id = row?.['id'] ?? row?.['usuarioId'];
    return id === null || id === undefined ? '' : String(id);
  }

  canEditRow(row: DataRow): boolean {
    const key = this.activeModule()?.key;
    if (
      !this.rowId(row) ||
      this.rowBoolean(row, 'activo') === false ||
      this.rowBoolean(row, 'activa') === false
    )
      return false;
    return (
      (this.user()?.rol === D && (key === 'productos' || key === 'donantes')) ||
      (this.user()?.rol === I && key === 'instituciones')
    );
  }

  canDeactivateRow(row: DataRow): boolean {
    const key = this.activeModule()?.key;
    if (
      !this.rowId(row) ||
      this.rowBoolean(row, 'activo') === false ||
      this.rowBoolean(row, 'activa') === false
    )
      return false;
    return (
      (this.user()?.rol === D && (key === 'productos' || key === 'donantes')) ||
      (this.user()?.rol === I && key === 'instituciones')
    );
  }

  canChangePayment(row: DataRow): boolean {
    return (
      this.activeModule()?.key === 'instituciones' &&
      this.user()?.rol === T &&
      Boolean(this.rowId(row))
    );
  }

  canManageRemito(row: DataRow): boolean {
    return (
      this.activeModule()?.key === 'remitos' &&
      this.user()?.rol === D &&
      Boolean(this.rowId(row)) &&
      this.rowText(row, 'estado').toUpperCase() === 'BORRADOR'
    );
  }

  roleLabel(role: Role | undefined): string {
    if (role === D) return 'Depósito';
    if (role === I) return 'Institucional';
    if (role === T) return 'Tesorería';
    return '';
  }

  logout(): void {
    sessionStorage.removeItem('bda-token');
    this.user.set(null);
    this.activeModule.set(null);
    this.payload.set(null);
    this.error.set('');
    this.closeForm();
  }

  private async loadModule(page: number): Promise<void> {
    const module = this.activeModule();
    if (!module) return;
    if (module.key === 'usuarios') {
      this.payload.set([]);
      return;
    }
    this.payload.set(null);
    this.error.set('');
    this.busy.set(true);
    try {
      const result = await firstValueFrom(
        this.http.get<unknown>(
          `${module.endpoint}?pagina=${page}&tamano=20`,
          this.requestOptions(),
        ),
      );
      this.payload.set(result);
      this.pageIndex.set(this.readNumber(result, 'pagina', 'number', page));
      this.totalPages.set(this.readNumber(result, 'totalPaginas', 'totalPages', 1));
    } catch (error) {
      this.showError(error, 'No se pudo cargar el módulo.');
    } finally {
      this.busy.set(false);
    }
  }

  private async loadIdentity(token: string): Promise<void> {
    try {
      const user = await firstValueFrom(
        this.http.get<User>('/api/auth/yo', {
          headers: { Authorization: `Bearer ${token}` },
        }),
      );
      this.user.set(user);
      this.activeModule.set(null);
    } catch (error) {
      sessionStorage.removeItem('bda-token');
      this.user.set(null);
      this.showError(error, 'La sesión venció o no es válida. Iniciá sesión nuevamente.');
    }
  }

  private async performAction(
    url: string,
    method: 'post' | 'delete',
    body: unknown,
    fallback: string,
  ): Promise<void> {
    this.error.set('');
    this.busy.set(true);
    try {
      await firstValueFrom(
        method === 'post'
          ? this.http.post<unknown>(url, body, this.requestOptions())
          : this.http.delete<unknown>(url, this.requestOptions()),
      );
      await this.loadModule(this.pageIndex());
    } catch (error) {
      this.showError(error, fallback);
    } finally {
      this.busy.set(false);
    }
  }

  private formPayload(key: ModuleKey, mode: FormMode, values: Record<string, string>): unknown {
    const numberValue = (name: string): number => Number(values[name]);
    if (key === 'productos' || key === 'donantes' || key === 'instituciones') return values;
    if (key === 'usuarios') {
      const rol = values['rol'] as Role;
      return {
        nombreUsuario: values['nombreUsuario'],
        clave: values['clave'],
        rol,
        institucionId: rol === I ? numberValue('institucionId') : null,
      };
    }
    if (key === 'donaciones') {
      return {
        donanteId: numberValue('donanteId'),
        fecha: values['fecha'],
        items: this.donationItems().map((item) => ({
          productoId: Number(item.productoId),
          cantidad: Number(item.cantidad),
          fechaVencimiento: item.fechaVencimiento,
        })),
      };
    }
    if (key === 'remitos' && mode === 'remito-detail') {
      return { stockId: numberValue('stockId'), cantidad: numberValue('cantidad') };
    }
    if (key === 'remitos')
      return { institucionId: numberValue('institucionId'), fechaEntrega: values['fechaEntrega'] };
    if (key === 'ajustes') {
      return {
        stockId: numberValue('stockId'),
        tipo: values['tipo'],
        cantidad: numberValue('cantidad'),
        motivo: values['motivo'],
      };
    }
    return {};
  }

  private localDate(date = new Date()): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private beginForm(mode: FormMode, row: DataRow | null, values: Record<string, string>): void {
    this.error.set('');
    this.editingRow.set(row);
    this.formValues.set(values);
    this.formMode.set(mode);
  }

  private defaultFormValues(): Record<string, string> {
    if (this.activeModule()?.key === 'donaciones') {
      return { fecha: this.localDate() };
    }
    if (this.activeModule()?.key === 'remitos') {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      return { fechaEntrega: this.localDate(tomorrow) };
    }
    if (this.activeModule()?.key === 'usuarios') return { rol: 'USER_DEPOSITO' };
    if (this.activeModule()?.key === 'instituciones' && this.user()?.rol === T)
      return { estadoPago: 'AL_DIA' };
    return {};
  }

  private valuesFromRow(row: DataRow): Record<string, string> {
    const fields =
      this.activeModule()?.key === 'productos'
        ? ['nombre', 'descripcion', 'tipoAlimento', 'unidadMedida']
        : this.activeModule()?.key === 'donantes'
          ? ['nombre', 'correo', 'telefono']
          : ['nombre', 'domicilio', 'tipo'];
    return Object.fromEntries(fields.map((field) => [field, this.rowText(row, field)]));
  }

  private requestOptions(): { headers: Record<string, string> } {
    const token = sessionStorage.getItem('bda-token');
    return { headers: token ? { Authorization: `Bearer ${token}` } : {} };
  }

  private showError(error: unknown, fallback: string): void {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      this.logout();
      this.error.set(
        'La sesión venció o las credenciales no son válidas. Iniciá sesión nuevamente.',
      );
    } else if (error instanceof HttpErrorResponse && error.status === 403) {
      this.error.set('Tu usuario no tiene permisos para esta operación.');
    } else if (error instanceof HttpErrorResponse && error.status === 0) {
      this.error.set('No hay conexión con la API. Verificá que el backend esté iniciado.');
    } else if (
      error instanceof HttpErrorResponse &&
      error.error &&
      typeof error.error === 'object' &&
      'message' in error.error
    ) {
      this.error.set(String(error.error['message']));
    } else {
      this.error.set(fallback);
    }
  }

  private isDataRow(value: unknown): value is DataRow {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }

  private rowText(row: DataRow, key: string): string {
    const value = row[key];
    return value === null || value === undefined ? '' : String(value);
  }

  private rowBoolean(row: DataRow, key: string): boolean | null {
    const value = row[key];
    return typeof value === 'boolean' ? value : null;
  }

  private firstText(row: DataRow, keys: string[], fallback: string): string {
    for (const key of keys) {
      const value = row[key];
      if (typeof value === 'string' && value.length > 0) return value;
    }
    return fallback;
  }

  private humanize(value: string): string {
    return value
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (letter) => letter.toLocaleUpperCase('es-AR'));
  }

  private readNumber(
    value: unknown,
    preferredKey: string,
    fallbackKey: string,
    fallback: number,
  ): number {
    if (!this.isDataRow(value)) return fallback;
    const result = value[preferredKey] ?? value[fallbackKey];
    return typeof result === 'number' ? result : fallback;
  }
}
