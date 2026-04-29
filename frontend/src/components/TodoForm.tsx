import { useState } from 'react';
import { Tag, Todo } from '../types';

interface FormData {
  title: string;
  description: string;
  priority: Todo['priority'];
  dueDate: string;
  tagIds: number[];
}

interface Props {
  initialValues?: Partial<Todo>;
  tags: Tag[];
  onSubmit: (data: FormData) => void;
  onCancel: () => void;
}

function TodoForm({ initialValues, tags, onSubmit, onCancel }: Props) {
  const [title, setTitle] = useState(initialValues?.title ?? '');
  const [description, setDescription] = useState(initialValues?.description ?? '');
  const [priority, setPriority] = useState<Todo['priority']>(initialValues?.priority ?? 'MEDIUM');
  const [dueDate, setDueDate] = useState(initialValues?.dueDate ?? '');
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>(
    initialValues?.tags?.map(t => t.id) ?? []
  );
  const [titleError, setTitleError] = useState('');

  const toggleTag = (tagId: number) => {
    setSelectedTagIds(prev =>
      prev.includes(tagId) ? prev.filter(id => id !== tagId) : [...prev, tagId]
    );
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setTitleError('Titel darf nicht leer sein');
      return;
    }
    onSubmit({ title, description, priority, dueDate, tagIds: selectedTagIds });
  };

  return (
    <form onSubmit={handleSubmit} className="todo-form">
      <label>
        Title
        <input value={title} onChange={e => { setTitle(e.target.value); setTitleError(''); }} />
        {titleError && <span className="field-error">{titleError}</span>}
      </label>
      <label>
        Description
        <textarea rows={3} value={description} onChange={e => setDescription(e.target.value)} />
      </label>
      <label>
        Priority
        <select value={priority} onChange={e => setPriority(e.target.value as Todo['priority'])}>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </select>
      </label>
      <label>
        Due date
        <input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} />
      </label>
      <fieldset>
        <legend>Tags</legend>
        {tags.map(tag => (
          <label key={tag.id} style={{ marginRight: 8, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <input type="checkbox" checked={selectedTagIds.includes(tag.id)} onChange={() => toggleTag(tag.id)} />
            {tag.name}
          </label>
        ))}
      </fieldset>
      <div className="form-actions">
        <button type="button" onClick={onCancel}>Cancel</button>
        <button type="submit" className="primary">Save</button>
      </div>
    </form>
  );
}

export default TodoForm;
