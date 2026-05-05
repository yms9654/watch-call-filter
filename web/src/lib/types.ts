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

export type BlockReason = 'unknown_number' | 'private_number';

export interface BlockLogEntry {
  id: string;
  e164: string | null;
  reason: BlockReason;
  blockedAt: number;
}
