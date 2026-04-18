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

export interface LoanRecord {
  id: string;
  loanDate: string;
  expectedReturnDate: string;
  actualReturnDate: string | null;
  status: string;
  person: PersonSummary;
  equipmentList: EquipmentSummary[];
}

export interface LoanRecordCreateDto {
  personId: string;
  equipmentIds: string[];
  expectedReturnDate: string;
}

export interface LoanRecordUpdateDto {
  expectedReturnDate?: string | null;
  actualReturnDate?: string | null;
  status?: string | null;
}
