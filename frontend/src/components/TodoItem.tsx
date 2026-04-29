import { Todo } from '../types';

interface Props {
  todo: Todo;
  onToggleDone: (t: Todo) => void;
  onDelete: (id: number) => void;
  onEdit: (t: Todo) => void;
}

function TodoItem({ todo, onToggleDone, onDelete, onEdit }: Props) {
  const dueText = todo.dueDate ? todo.dueDate.split('-').reverse().join('.') : '—';
  const priorityColor = todo.priority === 'HIGH' ? 'red' : todo.priority === 'MEDIUM' ? 'orange' : 'gray';

  return (
    <li className="todo-item" style={{ opacity: todo.done ? 0.5 : 1 }}>
      <input
        type="checkbox"
        checked={todo.done}
        onChange={() => onToggleDone(todo)}
      />
      <div className="todo-body">
        <div className="todo-title" style={{ textDecoration: todo.done ? 'line-through' : 'none' }}>
          {todo.title}
        </div>
        {todo.description && <div className="todo-desc">{todo.description}</div>}
        <div className="todo-meta">
          <span style={{ color: priorityColor, fontWeight: 'bold' }}>{todo.priority}</span>
          <span>· due: {dueText}</span>
          {(todo.tags || []).map((tag: any) => (
            <span key={tag.id} className="tag-chip">#{tag.name}</span>
          ))}
        </div>
      </div>
      <div className="todo-actions">
        <button onClick={() => onEdit(todo)}>Edit</button>
        <button onClick={() => onDelete(todo.id)} className="danger">Delete</button>
      </div>
    </li>
  );
}

export default TodoItem;
