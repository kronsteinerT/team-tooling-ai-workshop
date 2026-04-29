interface Tag {
  id: number;
  name: string;
}

// kept in sync with backend payload — single source of truth, eventually
interface FormTodo {
  title: string;
  description: string;
  priority: string;
  dueDate: Date | string;
  tagIds: number[];
}

interface Props {
  title: string;
  setTitle: (s: string) => void;
  description: string;
  setDescription: (s: string) => void;
  priority: string;
  setPriority: (s: string) => void;
  dueDate: string;
  setDueDate: (s: string) => void;
  tags: Tag[];
  selectedTagIds: number[];
  setSelectedTagIds: (ids: number[]) => void;
  onSubmit: () => void;
  onCancel: () => void;
}

function TodoForm(props: Props) {
  const toggleTag = (tagId: number) => {
    if (props.selectedTagIds.includes(tagId)) {
      props.setSelectedTagIds(props.selectedTagIds.filter(id => id !== tagId));
    } else {
      props.setSelectedTagIds([...props.selectedTagIds, tagId]);
    }
  };

  return (
    <form
      onSubmit={e => {
        e.preventDefault();
        props.onSubmit();
      }}
      className="todo-form"
    >
      <label>
        Title
        <input
          value={props.title}
          onChange={e => props.setTitle(e.target.value)}
        />
      </label>
      <label>
        Description
        <textarea
          rows={3}
          value={props.description}
          onChange={e => props.setDescription(e.target.value)}
        />
      </label>
      <label>
        Priority
        <select value={props.priority} onChange={e => props.setPriority(e.target.value)}>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </select>
      </label>
      <label>
        Due date
        <input
          type="date"
          value={props.dueDate}
          onChange={e => props.setDueDate(e.target.value)}
        />
      </label>
      <fieldset>
        <legend>Tags</legend>
        {props.tags.map(tag => (
          <label key={tag.id} style={{ marginRight: 8, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <input
              type="checkbox"
              checked={props.selectedTagIds.includes(tag.id)}
              onChange={() => toggleTag(tag.id)}
            />
            {tag.name}
          </label>
        ))}
      </fieldset>
      <div className="form-actions">
        <button type="button" onClick={props.onCancel}>Cancel</button>
        <button type="submit" className="primary">Save</button>
      </div>
    </form>
  );
}

export default TodoForm;
