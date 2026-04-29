import { useState, useEffect } from 'react';
import FilterBar from './components/FilterBar';
import TodoList from './components/TodoList';
import TodoForm from './components/TodoForm';
import Sidebar from './components/Sidebar';
import { Todo, Tag } from './types';
import { getTodos, createTodo, updateTodo, deleteTodo, getTags } from './api';

interface Filters {
  done: string;
  priority: string;
  tagId: number | null;
  page: number;
}

const PAGE_SIZE = 20;

function App() {
  const [todos, setTodos] = useState<Todo[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [filters, setFilters] = useState<Filters>({ done: 'all', priority: 'all', tagId: null, page: 0 });
  const [searchTerm, setSearchTerm] = useState('');
  const [totalCount, setTotalCount] = useState(0);
  const [doneCount, setDoneCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingTodo, setEditingTodo] = useState<Todo | null>(null);

  useEffect(() => {
    getTags().then(setTags).catch(console.error);
  }, []);

  useEffect(() => {
    const params = new URLSearchParams();
    if (filters.done !== 'all') params.set('done', filters.done === 'done' ? 'true' : 'false');
    if (filters.priority !== 'all') params.set('priority', filters.priority);
    if (filters.tagId !== null) params.set('tagId', String(filters.tagId));
    params.set('page', String(filters.page));
    params.set('size', String(PAGE_SIZE));

    setLoading(true);
    setError(null);
    getTodos(params)
      .then(setTodos)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => {
    getTodos(new URLSearchParams({ size: '1000' })).then(data => {
      setTotalCount(data.length);
      setDoneCount(data.filter(t => t.done).length);
    }).catch(console.error);
  }, [todos]);

  const handleToggleDone = (todo: Todo) => {
    updateTodo(todo.id, { ...todo, done: !todo.done, tagIds: todo.tags.map(t => t.id) })
      .then(updated => setTodos(prev => prev.map(t => t.id === updated.id ? updated : t)))
      .catch(console.error);
  };

  const handleDelete = (id: number) => {
    deleteTodo(id)
      .then(() => setFilters(f => ({ ...f })))
      .catch(console.error);
  };

  const handleSubmit = (data: { title: string; description: string; priority: Todo['priority']; dueDate: string; tagIds: number[] }) => {
    const body = { ...data, dueDate: data.dueDate || undefined };
    const apiCall = editingTodo ? updateTodo(editingTodo.id, body) : createTodo(body);
    apiCall
      .then(() => { setShowForm(false); setFilters(f => ({ ...f })); })
      .catch(console.error);
  };

  const visibleTodos = searchTerm
    ? todos.filter(t => t.title.toLowerCase().includes(searchTerm.toLowerCase()))
    : todos;

  return (
    <div className="app" style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar
        tags={tags}
        selectedTagId={filters.tagId}
        onSelectTag={tagId => setFilters(f => ({ ...f, tagId, page: 0 }))}
        onTagsChanged={() => getTags().then(setTags).catch(console.error)}
        totalCount={totalCount}
        doneCount={doneCount}
      />

      <main className="main">
        <header className="header">
          <h1>Todos</h1>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              type="text"
              placeholder="Search…"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              style={{ padding: '6px 10px', border: '1px solid #ccc', borderRadius: 4, fontSize: 13 }}
            />
            <button onClick={() => { setEditingTodo(null); setShowForm(true); }} className="primary">
              + New Todo
            </button>
          </div>
        </header>

        <FilterBar
          doneFilter={filters.done}
          setDoneFilter={done => setFilters(f => ({ ...f, done, page: 0 }))}
          priorityFilter={filters.priority}
          setPriorityFilter={priority => setFilters(f => ({ ...f, priority, page: 0 }))}
          selectedTagId={filters.tagId}
          tags={tags}
        />

        {loading && <p style={{ color: '#888' }}>Laden…</p>}
        {error && <p style={{ color: 'red' }}>Fehler: {error}</p>}
        {!loading && !error && (
          <TodoList
            todos={visibleTodos}
            onToggleDone={handleToggleDone}
            onDelete={handleDelete}
            onEdit={todo => { setEditingTodo(todo); setShowForm(true); }}
          />
        )}

        <div className="pagination">
          <button onClick={() => setFilters(f => ({ ...f, page: Math.max(0, f.page - 1) }))} disabled={filters.page === 0}>Prev</button>
          <span style={{ margin: '0 12px' }}>Page {filters.page + 1}</span>
          <button onClick={() => setFilters(f => ({ ...f, page: f.page + 1 }))}>Next</button>
        </div>

        {showForm && (
          <div className="modal-overlay" onClick={() => setShowForm(false)}>
            <div className="modal" onClick={e => e.stopPropagation()}>
              <h3>{editingTodo ? 'Edit todo' : 'New todo'}</h3>
              <TodoForm
                initialValues={editingTodo ?? undefined}
                tags={tags}
                onSubmit={handleSubmit}
                onCancel={() => setShowForm(false)}
              />
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
