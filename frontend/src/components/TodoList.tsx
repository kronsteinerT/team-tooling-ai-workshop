import TodoItem from './TodoItem';
import { Todo } from '../types';

interface Props {
  todos: Todo[];
  onToggleDone: (t: Todo) => void;
  onDelete: (id: number) => void;
  onEdit: (t: Todo) => void;
}

function TodoList({ todos, onToggleDone, onDelete, onEdit }: Props) {
  if (todos.length === 0) {
    return <p className="status-empty">Keine Todos gefunden.</p>;
  }
  return (
    <ul className="todo-list">
      {todos.map((todo) => (
        <TodoItem
          key={todo.id}
          todo={todo}
          onToggleDone={onToggleDone}
          onDelete={onDelete}
          onEdit={onEdit}
        />
      ))}
    </ul>
  );
}

export default TodoList;
