export function toE164Kr(raw: string): string | null {
  const digits = raw.replace(/[^\d+]/g, '');
  if (!digits) return null;
  if (digits.startsWith('+')) return digits;
  if (digits.startsWith('0') && digits.length >= 10) {
    return '+82' + digits.substring(1);
  }
  return digits;
}

export function formatKr(e164: string): string {
  if (e164.startsWith('+82')) {
    const local = '0' + e164.substring(3);
    if (local.length === 11) return `${local.slice(0, 3)}-${local.slice(3, 7)}-${local.slice(7)}`;
    if (local.length === 10) return `${local.slice(0, 2)}-${local.slice(2, 6)}-${local.slice(6)}`;
  }
  return e164;
}
