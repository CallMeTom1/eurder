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
} as const;
