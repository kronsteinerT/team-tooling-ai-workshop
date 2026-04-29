import TodoItem from './TodoItem';

interface Props {
  todos: any[];
  onToggleDone: (t: any) => void;
  onDelete: (id: number) => void;
  onEdit: (t: any) => void;
  selectedFilter: string;
}

function TodoList({ todos, onToggleDone, onDelete, onEdit, selectedFilter }: Props) {
  if (todos.length === 0) {
    return <p style={{ color: '#888' }}>Keine Todos.</p>;
  }
  return (
    <ul className="todo-list">
      {todos.map((todo, i) => (
        <TodoItem
          key={i}
          todo={todo}
          onToggleDone={onToggleDone}
          onDelete={onDelete}
          onEdit={onEdit}
          selectedFilter={selectedFilter}
        />
      ))}
    </ul>
  );
}

export default TodoList;
