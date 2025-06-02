interface ApiResponse<T = any> {
  data: T;
}

export const validateToken = (): boolean => {
  return !!localStorage.getItem('token') && !!localStorage.getItem('usuario');
};

export const validateResponse = <T>(
  response: ApiResponse<T>,
  expectedType: 'array' | 'object' = 'array'
): true => {
  if (expectedType === 'array' && !Array.isArray(response.data)) {
    throw new Error('La respuesta no es un array válido');
  }
  // Puedes agregar más validaciones para otros tipos si quieres
  return true;
};