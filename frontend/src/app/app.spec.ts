import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideHttpClient } from '@angular/common/http';

describe('App', () => {
  beforeEach(async () => {
    sessionStorage.removeItem('bda-token');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient()],
    }).compileComponents();
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
});
