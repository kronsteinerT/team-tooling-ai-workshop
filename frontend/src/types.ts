export interface Todo {
  id: number;
  title: string;
  description: string;
  done: boolean;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  dueDate: string;
  createdAt: string;
  tags: Tag[];
}

export interface Tag {
  id: number;
  name: string;
}
