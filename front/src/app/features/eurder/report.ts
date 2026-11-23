import type { ItemGroupReportDto, ReportDto } from '@core/api/eurder-api';

export interface ReportLine {
  itemName: string;
  itemState: ItemGroupReportDto['itemState'];
  amount: number;
  subtotalInCents: number;
}

export interface ReportedEurder {
  id: string;
  lines: ReportLine[];
  totalInCents: number;
}

export interface Report {
  eurders: ReportedEurder[];
  totalInCents: number;
}

const CENTS_PER_EURO = 100;

function inCents(euros: number): number {
  return Math.round(euros * CENTS_PER_EURO);
}

export function toReport(dto: ReportDto): Report {
  return {
    eurders: dto.eurders.map((eurder) => ({
      id: eurder.id,
      lines: eurder.itemGroups.map((group) => ({
        itemName: group.itemName,
        itemState: group.itemState,
        amount: group.amount,
        subtotalInCents: inCents(group.subtotal),
      })),
      totalInCents: inCents(eurder.totalPrice),
    })),
    totalInCents: inCents(dto.totalPrice),
  };
}
