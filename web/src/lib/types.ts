export interface Watch {
  id: string;
  label: string;
  pairedAt: number;
}

export interface AllowlistEntry {
  id: string;
  name: string;
  e164: string;
  addedAt: number;
}
