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
  },
  items: {
    all: `${API}/items`,
    one: (id: string) => `${API}/items/${id}`,
    units: (id: string) => `${API}/items/${id}/units`,
  },
  eurders: {
    all: `${API}/eurders`,
  },
} as const;
