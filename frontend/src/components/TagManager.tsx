import { useState } from 'react';
import { Tag } from '../types';
import { createTag, deleteTag } from '../api';

interface Props {
  tags: Tag[];
  onChanged: () => void;
}

function TagManager({ tags, onChanged }: Props) {
  const [newName, setNewName] = useState('');

  const handleAdd = () => {
    createTag(newName).then(() => { setNewName(''); onChanged(); }).catch(console.error);
  };

  const handleDelete = (id: number) => {
    deleteTag(id).then(onChanged).catch(console.error);
  };

  return (
    <div className="tag-manager">
      <div className="tag-manager-add">
        <input
          placeholder="new tag name"
          value={newName}
          onChange={e => setNewName(e.target.value)}
        />
        <button onClick={handleAdd} className="primary small">Add</button>
      </div>
      <ul>
        {tags.map((tag) => (
          <li key={tag.id}>
            <span>{tag.name}</span>
            <button onClick={() => handleDelete(tag.id)} className="danger small">×</button>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default TagManager;
