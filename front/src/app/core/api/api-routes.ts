export const API = '/api';

export const ApiRoutes = {
  auth: {
    login: `${API}/auth/login`,
    refresh: `${API}/auth/refresh`,
    logout: `${API}/auth/logout`,
    me: `${API}/auth/me`,
  },
  catalog: {
    all: `${API}/catalog`,
    one: (id: string) => `${API}/catalog/${id}`,
  },
  customers: {
    all: `${API}/customers`,
    one: (id: string) => `${API}/customers/${id}`,
  },
  items: {
    all: `${API}/items`,
    one: (id: string) => `${API}/items/${id}`,
    units: (id: string) => `${API}/items/${id}/units`,
  },
  eurders: {
    all: `${API}/eurders`,
    report: `${API}/eurders/report`,
    reportOf: (customerId: string) => `${API}/eurders/reports/${customerId}`,
    shippingToday: `${API}/eurders/shipping-today`,
    reorder: (id: string) => `${API}/eurders/${id}/reorder`,
  },
} as const;
