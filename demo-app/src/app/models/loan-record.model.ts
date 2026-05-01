export interface PersonSummary {
  id: string;
  name: string;
  email: string;
  role: string;
  age: number;
  password: string;
}

export interface EquipmentSummary {
  id: string;
  name: string;
  description: string;
  stockCount: number;
}

export interface LoanEquipmentItem {
  id: string;
  equipment: EquipmentSummary;
  quantity: number;
}

export interface LoanRecord {
  id: string;
  loanDate: string;
  expectedReturnDate: string;
  actualReturnDate: string | null;
  status: string;
  person: PersonSummary;
  items: LoanEquipmentItem[];
}

export interface EquipmentQuantityDto {
  equipmentId: string;
  quantity: number;
}

export interface LoanRecordCreateDto {
  personId: string;
  equipmentQuantities: EquipmentQuantityDto[];
  expectedReturnDate: string;
}

export interface LoanRecordUpdateDto {
  expectedReturnDate?: string | null;
  actualReturnDate?: string | null;
  status?: string | null;
}

export interface StudentLoanRequestDto {
  equipmentQuantities: EquipmentQuantityDto[];
  expectedReturnDate: string;
}
