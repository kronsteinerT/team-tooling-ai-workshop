import { Todo, Tag } from './types';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, options);
  if (!res.ok) throw new Error(`API error ${res.status}`);
  return res.json();
}

export async function getTodos(params: URLSearchParams, signal?: AbortSignal): Promise<Todo[]> {
  return request(`/api/todos?${params}`, { signal });
}

export async function createTodo(body: Partial<Todo> & { tagIds: number[] }): Promise<Todo> {
  return request('/api/todos', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  });
}

export async function updateTodo(id: number, body: Partial<Todo> & { tagIds: number[] }): Promise<Todo> {
  return request(`/api/todos/${id}`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  });
}

export async function deleteTodo(id: number): Promise<void> {
  const res = await fetch(`/api/todos/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`API error ${res.status}`);
}

export async function getTags(): Promise<Tag[]> {
  return request('/api/tags');
}

export async function createTag(name: string): Promise<Tag> {
  return request('/api/tags', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ name }),
  });
}

export async function deleteTag(id: number): Promise<void> {
  const res = await fetch(`/api/tags/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`API error ${res.status}`);
}
