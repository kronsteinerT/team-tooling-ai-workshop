interface Tag {
  id: number;
  name: string;
}

interface Props {
  doneFilter: string;
  setDoneFilter: (s: string) => void;
  priorityFilter: string;
  setPriorityFilter: (s: string) => void;
  selectedTagId: number | null;
  tags: Tag[];
}

function FilterBar({ doneFilter, setDoneFilter, priorityFilter, setPriorityFilter, selectedTagId, tags }: Props) {
  const selectedTag = tags.find(t => t.id === selectedTagId);
  return (
    <div className="filter-bar">
      <label>
        Status:
        <select value={doneFilter} onChange={e => setDoneFilter(e.target.value)}>
          <option value="all">All</option>
          <option value="open">Open</option>
          <option value="done">Done</option>
        </select>
      </label>
      <label>
        Priority:
        <select value={priorityFilter} onChange={e => setPriorityFilter(e.target.value)}>
          <option value="all">All</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
      </label>
      {selectedTag && <span className="filter-pill">Tag: {selectedTag.name}</span>}
    </div>
  );
}

export default FilterBar;
