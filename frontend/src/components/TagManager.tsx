import { useState } from 'react';

interface Tag {
  id: number;
  name: string;
}

interface Props {
  tags: Tag[];
  onChanged: () => void;
}

function TagManager({ tags, onChanged }: Props) {
  const [newName, setNewName] = useState('');

  const handleAdd = () => {
    fetch('/api/tags', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: newName })
    }).then(() => {
      setNewName('');
      onChanged();
    });
  };

  const handleDelete = (id: number) => {
    fetch(`/api/tags/${id}`, { method: 'DELETE' }).then(() => onChanged());
  };

  return (
    <div className="tag-manager">
      <div className="tag-manager-add">
        <input
          placeholder="new tag name"
          value={newName}
          onChange={e => setNewName(e.target.value)}
        />
        <button onClick={handleAdd}>Add</button>
      </div>
      <ul>
        {tags.map((tag, i) => (
          <li key={i}>
            <span>{tag.name}</span>
            <button onClick={() => handleDelete(tag.id)} className="danger small">×</button>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default TagManager;
