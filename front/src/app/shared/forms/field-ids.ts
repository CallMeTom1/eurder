let forms = 0;

export function uniquePrefix(): string {
  forms += 1;
  return `form-${forms}-`;
}
