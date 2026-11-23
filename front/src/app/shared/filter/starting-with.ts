export function startingWith<T>(
  items: readonly T[],
  query: string,
  text: (item: T) => string,
): T[] {
  const start = query.trim().toLocaleLowerCase();
  return items.filter((item) => text(item).toLocaleLowerCase().startsWith(start));
}
