import { useState } from 'react';
import { Tag } from '../types';
import TagManager from './TagManager';

interface Props {
  tags: Tag[];
  selectedTagId: number | null;
  onSelectTag: (id: number | null) => void;
  onTagsChanged: () => void;
  totalCount: number;
  doneCount: number;
}

function Sidebar({ tags, selectedTagId, onSelectTag, onTagsChanged, totalCount, doneCount }: Props) {
  const [showTagManager, setShowTagManager] = useState(false);

  return (
    <aside className="sidebar">
      <h2>Tags</h2>
      <ul className="tag-list">
        <li>
          <button onClick={() => onSelectTag(null)} style={{ fontWeight: selectedTagId === null ? 'bold' : 'normal' }}>
            All
          </button>
        </li>
        {tags.map(tag => (
          <li key={tag.id}>
            <button onClick={() => onSelectTag(tag.id)} style={{ fontWeight: selectedTagId === tag.id ? 'bold' : 'normal' }}>
              {tag.name}
            </button>
          </li>
        ))}
      </ul>
      <button onClick={() => setShowTagManager(s => !s)}>
        {showTagManager ? 'Close tag manager' : 'Manage tags'}
      </button>
      {showTagManager && <TagManager tags={tags} onChanged={onTagsChanged} />}
      <div className="stats">
        <div className="stat"><span className="stat-label">Total</span><span className="stat-value">{totalCount}</span></div>
        <div className="stat"><span className="stat-label">Done</span><span className="stat-value">{doneCount}</span></div>
        <div className="stat"><span className="stat-label">Open</span><span className="stat-value">{totalCount - doneCount}</span></div>
      </div>
    </aside>
  );
}

export default Sidebar;
