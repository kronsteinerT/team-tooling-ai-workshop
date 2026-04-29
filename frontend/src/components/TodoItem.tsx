import { Todo } from '../types';

interface Props {
  todo: Todo;
  onToggleDone: (t: Todo) => void;
  onDelete: (id: number) => void;
  onEdit: (t: Todo) => void;
}

function TodoItem({ todo, onToggleDone, onDelete, onEdit }: Props) {
  const dueText = todo.dueDate ? todo.dueDate.split('-').reverse().join('.') : null;

  return (
    <li className={`todo-item${todo.done ? ' done' : ''}`}>
      <input type="checkbox" checked={todo.done} onChange={() => onToggleDone(todo)} />
      <div className="todo-body">
        <div className={`todo-title${todo.done ? ' strikethrough' : ''}`}>{todo.title}</div>
        {todo.description && <div className="todo-desc">{todo.description}</div>}
        <div className="todo-meta">
          <span className={`priority-badge ${todo.priority}`}>{todo.priority}</span>
          {todo.dueDate && <span>due {dueText}</span>}
          {todo.tags.map(tag => (
            <span key={tag.id} className="tag-chip">#{tag.name}</span>
          ))}
        </div>
      </div>
      <div className="todo-actions">
        <button className="small" onClick={() => onEdit(todo)}>Edit</button>
        <button className="small danger" onClick={() => onDelete(todo.id)}>Delete</button>
      </div>
    </li>
  );
}

export default TodoItem;
