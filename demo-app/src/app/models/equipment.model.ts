export interface Equipment {
  id: string;
  name: string;
  description: string;
  stockCount: number;
}

export type CreateEquipmentDto = Omit<Equipment, 'id'>;
export type UpdateEquipmentDto = Omit<Equipment, 'id'>;
