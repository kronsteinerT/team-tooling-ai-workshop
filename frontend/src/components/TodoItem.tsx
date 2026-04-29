interface Props {
  todo: any;
  onToggleDone: (t: any) => void;
  onDelete: (id: number) => void;
  onEdit: (t: any) => void;
  selectedFilter: string;
}

function TodoItem({ todo, onToggleDone, onDelete, onEdit, selectedFilter }: Props) {
  const dueText = todo.dueDate ? new Date(todo.dueDate).toLocaleDateString() : '—';
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
