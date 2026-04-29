import { useState, useEffect } from 'react';
import FilterBar from './components/FilterBar';
import TodoList from './components/TodoList';
import TodoForm from './components/TodoForm';
import TagManager from './components/TagManager';

interface Todo {
  id: number;
  title: string;
  description?: string;
  done: boolean;
  priority: string;
  dueDate?: string;
  createdAt?: string;
  tags?: any[];
}

interface Tag {
  id: number;
  name: string;
}

function App() {
  const [todos, setTodos] = useState<any[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [selectedTagId, setSelectedTagId] = useState<number | null>(null);
  const [doneFilter, setDoneFilter] = useState<string>('all');
  const [priorityFilter, setPriorityFilter] = useState<string>('all');
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [showForm, setShowForm] = useState(false);
  const [editingTodo, setEditingTodo] = useState<Todo | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [dueDate, setDueDate] = useState('');
  const [selectedFormTagIds, setSelectedFormTagIds] = useState<number[]>([]);
  const [showTagManager, setShowTagManager] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [totalCount, setTotalCount] = useState(0);
  const [doneCount, setDoneCount] = useState(0);

  useEffect(() => {
    fetch('/api/tags')
      .then(r => r.json())
      .then(data => setTags(data));
  }, []);

  useEffect(() => {
    const params = new URLSearchParams();
    if (doneFilter !== 'all') params.set('done', doneFilter === 'done' ? 'true' : 'false');
    if (priorityFilter !== 'all') params.set('priority', priorityFilter);
    if (selectedTagId !== null) params.set('tagId', String(selectedTagId));
    params.set('page', String(page));
    params.set('size', String(pageSize));

    fetch(`/api/todos?${params}`)
      .then(r => r.json())
      .then(data => {
        setTodos(data);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doneFilter, priorityFilter, selectedTagId, page]);

  useEffect(() => {
    fetch('/api/todos?size=1000')
      .then(r => r.json())
      .then((data: any[]) => {
        setTotalCount(data.length);
        setDoneCount(data.filter(t => t.done).length);
      });
  }, [todos]);

  const refetchTodos = () => {
    const params = new URLSearchParams();
    if (doneFilter !== 'all') params.set('done', doneFilter === 'done' ? 'true' : 'false');
    if (priorityFilter !== 'all') params.set('priority', priorityFilter);
    if (selectedTagId !== null) params.set('tagId', String(selectedTagId));
    params.set('page', String(page));
    params.set('size', String(pageSize));
    fetch(`/api/todos?${params}`).then(r => r.json()).then(data => setTodos(data));
  };

  const handleToggleDone = (todo: any) => {
    fetch(`/api/todos/${todo.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...todo,
        done: !todo.done,
        tagIds: (todo.tags || []).map((t: any) => t.id)
      })
    })
      .then(r => r.json())
      .then(updated => {
        setTodos(prev => prev.map(t => t.id === updated.id ? updated : t));
      });
  };

  const handleDelete = (id: number) => {
    fetch(`/api/todos/${id}`, { method: 'DELETE' }).then(() => refetchTodos());
  };

  const handleEdit = (todo: any) => {
    setEditingTodo(todo);
    setTitle(todo.title);
    setDescription(todo.description || '');
    setPriority(todo.priority || 'MEDIUM');
    setDueDate(todo.dueDate || '');
    setSelectedFormTagIds((todo.tags || []).map((t: any) => t.id));
    setShowForm(true);
  };

  const handleNew = () => {
    setEditingTodo(null);
    setTitle('');
    setDescription('');
    setPriority('MEDIUM');
    setDueDate('');
    setSelectedFormTagIds([]);
    setShowForm(true);
  };

  const handleSubmit = () => {
    const body = {
      title,
      description,
      priority,
      dueDate: dueDate || null,
      tagIds: selectedFormTagIds
    };
    if (editingTodo) {
      fetch(`/api/todos/${editingTodo.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
        .then(r => r.json())
        .then(() => {
          setShowForm(false);
          refetchTodos();
        });
    } else {
      fetch('/api/todos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
        .then(r => r.json())
        .then(() => {
          setShowForm(false);
          refetchTodos();
        });
    }
  };

  const refreshTags = () => {
    fetch('/api/tags').then(r => r.json()).then(setTags);
  };

  const visibleTodos = searchTerm
    ? todos.filter(t => t.title.toLowerCase().includes(searchTerm.toLowerCase()))
    : todos;

  return (
    <div className="app" style={{ display: 'flex', minHeight: '100vh' }}>
      <aside className="sidebar">
        <h2>Tags</h2>
        <ul className="tag-list">
          <li>
            <button
              onClick={() => setSelectedTagId(null)}
              style={{ fontWeight: selectedTagId === null ? 'bold' : 'normal' }}
            >
              All
            </button>
          </li>
          {tags.map(tag => (
            <li key={tag.id}>
              <button
                onClick={() => setSelectedTagId(tag.id)}
                style={{ fontWeight: selectedTagId === tag.id ? 'bold' : 'normal' }}
              >
                {tag.name}
              </button>
            </li>
          ))}
        </ul>
        <button onClick={() => setShowTagManager(!showTagManager)}>
          {showTagManager ? 'Close tag manager' : 'Manage tags'}
        </button>
        {showTagManager && <TagManager tags={tags} onChanged={refreshTags} />}

        <div className="stats">
          <div className="stat">
            <span className="stat-label">Total</span>
            <span className="stat-value">{totalCount}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Done</span>
            <span className="stat-value">{doneCount}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Open</span>
            <span className="stat-value">{totalCount - doneCount}</span>
          </div>
        </div>
      </aside>

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
            <button onClick={handleNew} className="primary">+ New Todo</button>
          </div>
        </header>

        <FilterBar
          doneFilter={doneFilter}
          setDoneFilter={setDoneFilter}
          priorityFilter={priorityFilter}
          setPriorityFilter={setPriorityFilter}
          selectedTagId={selectedTagId}
          tags={tags}
        />

        <TodoList
          todos={visibleTodos}
          onToggleDone={handleToggleDone}
          onDelete={handleDelete}
          onEdit={handleEdit}
          selectedFilter={doneFilter}
        />

        <div className="pagination">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Prev</button>
          <span style={{ margin: '0 12px' }}>Page {page + 1}</span>
          <button onClick={() => setPage(p => p + 1)}>Next</button>
        </div>

        {showForm && (
          <div className="modal-overlay" onClick={() => setShowForm(false)}>
            <div className="modal" onClick={e => e.stopPropagation()}>
              <h3>{editingTodo ? 'Edit todo' : 'New todo'}</h3>
              <TodoForm
                title={title}
                setTitle={setTitle}
                description={description}
                setDescription={setDescription}
                priority={priority}
                setPriority={setPriority}
                dueDate={dueDate}
                setDueDate={setDueDate}
                tags={tags}
                selectedTagIds={selectedFormTagIds}
                setSelectedTagIds={setSelectedFormTagIds}
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
