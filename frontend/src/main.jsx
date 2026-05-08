import React, { useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const strategies = ['BASIC_LOOP', 'GROUP_BY_QUERY', 'BULK_SAVE', 'INDEX_APPLIED'];

const strategyProfiles = {
  BASIC_LOOP: {
    processedCount: 100000,
    paymentAmount: 850000000,
    cancelAmount: 43000000,
    feeAmount: 24210000,
    settlementAmount: 782790000,
    elapsedMs: 3520,
  },
  GROUP_BY_QUERY: {
    processedCount: 100000,
    paymentAmount: 850000000,
    cancelAmount: 43000000,
    feeAmount: 24210000,
    settlementAmount: 782790000,
    elapsedMs: 810,
  },
  BULK_SAVE: {
    processedCount: 100000,
    paymentAmount: 850000000,
    cancelAmount: 43000000,
    feeAmount: 24210000,
    settlementAmount: 782790000,
    elapsedMs: 620,
  },
  INDEX_APPLIED: {
    processedCount: 100000,
    paymentAmount: 850000000,
    cancelAmount: 43000000,
    feeAmount: 24210000,
    settlementAmount: 782790000,
    elapsedMs: 410,
  },
};

const merchantRows = [
  {
    merchantName: '핀코페이 강남점',
    paymentAmount: 182500000,
    cancelAmount: 8200000,
    feeRate: 0.028,
  },
  {
    merchantName: '에이치마트 온라인',
    paymentAmount: 156200000,
    cancelAmount: 5100000,
    feeRate: 0.031,
  },
  {
    merchantName: '라이트커머스',
    paymentAmount: 128900000,
    cancelAmount: 7300000,
    feeRate: 0.026,
  },
  {
    merchantName: '모던서플라이',
    paymentAmount: 96700000,
    cancelAmount: 3900000,
    feeRate: 0.024,
  },
  {
    merchantName: '브릿지스토어',
    paymentAmount: 74200000,
    cancelAmount: 2400000,
    feeRate: 0.029,
  },
];

const historyRows = [
  {
    executedAt: '2026-05-08 14:30:10',
    settlementDate: '2026-05-08',
    strategy: 'BASIC_LOOP',
    processedCount: 100000,
    successCount: 99720,
    failureCount: 280,
    elapsedMs: 3520,
    status: 'SUCCESS',
  },
  {
    executedAt: '2026-05-08 14:38:42',
    settlementDate: '2026-05-08',
    strategy: 'GROUP_BY_QUERY',
    processedCount: 100000,
    successCount: 99880,
    failureCount: 120,
    elapsedMs: 810,
    status: 'SUCCESS',
  },
  {
    executedAt: '2026-05-08 14:47:21',
    settlementDate: '2026-05-08',
    strategy: 'BULK_SAVE',
    processedCount: 100000,
    successCount: 99910,
    failureCount: 90,
    elapsedMs: 620,
    status: 'SUCCESS',
  },
  {
    executedAt: '2026-05-08 14:55:33',
    settlementDate: '2026-05-08',
    strategy: 'INDEX_APPLIED',
    processedCount: 100000,
    successCount: 99936,
    failureCount: 64,
    elapsedMs: 410,
    status: 'SUCCESS',
  },
];

const formatWon = (value) =>
  new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(value);

const formatCount = (value) => `${new Intl.NumberFormat('ko-KR').format(value)}건`;
const formatMs = (value) => `${new Intl.NumberFormat('ko-KR').format(value)}ms`;
const formatPercent = (value) => `${(value * 100).toFixed(1)}%`;

function App() {
  const [settlementDate, setSettlementDate] = useState('2026-05-08');
  const [strategy, setStrategy] = useState('BASIC_LOOP');
  const [lastRun, setLastRun] = useState(historyRows[0]);

  const summary = strategyProfiles[strategy];

  const settlementRows = useMemo(
    () =>
      merchantRows.map((row) => {
        const netSales = row.paymentAmount - row.cancelAmount;
        const feeAmount = Math.round(netSales * row.feeRate);
        return {
          ...row,
          netSales,
          feeAmount,
          settlementAmount: netSales - feeAmount,
        };
      }),
    [],
  );

  const handleRun = () => {
    setLastRun({
      executedAt: new Date().toLocaleString('sv-SE').replace('T', ' '),
      settlementDate,
      strategy,
      processedCount: summary.processedCount,
      successCount: summary.processedCount - 72,
      failureCount: 72,
      elapsedMs: summary.elapsedMs,
      status: 'SUCCESS',
    });
  };

  return (
    <main className="min-h-screen bg-panel text-ink">
      <section className="border-b border-line bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-6">
          <div className="flex flex-col gap-2">
            <p className="text-sm font-semibold text-accent">Settlement Performance</p>
            <h1 className="text-2xl font-semibold tracking-normal text-ink">정산 성능 대시보드</h1>
            <p className="max-w-3xl text-sm leading-6 text-muted">
              대용량 정산 배치를 실행하고 처리 전략별 결과와 실행 시간을 비교하는 업무 대시보드입니다.
            </p>
          </div>

          <div className="grid gap-3 rounded-md border border-line bg-panel p-4 lg:grid-cols-[180px_220px_1fr_auto]">
            <label className="flex flex-col gap-2 text-sm font-medium text-muted">
              정산일자
              <input
                className="h-11 rounded-md border border-line bg-white px-3 text-sm font-semibold text-ink outline-none focus:border-accent"
                type="date"
                value={settlementDate}
                onChange={(event) => setSettlementDate(event.target.value)}
              />
            </label>

            <label className="flex flex-col gap-2 text-sm font-medium text-muted">
              처리 전략
              <select
                className="h-11 rounded-md border border-line bg-white px-3 text-sm font-semibold text-ink outline-none focus:border-accent"
                value={strategy}
                onChange={(event) => setStrategy(event.target.value)}
              >
                {strategies.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </label>

            <div className="flex items-end">
              <div className="w-full rounded-md border border-line bg-white px-4 py-3">
                <p className="text-xs font-semibold text-muted">최근 실행</p>
                <p className="mt-1 text-sm font-semibold text-ink">
                  {lastRun.strategy} · {formatCount(lastRun.processedCount)} · {formatMs(lastRun.elapsedMs)}
                </p>
              </div>
            </div>

            <button
              className="h-11 self-end rounded-md bg-accent px-5 text-sm font-semibold text-white hover:bg-teal-800"
              type="button"
              onClick={handleRun}
            >
              정산 배치 실행
            </button>
          </div>
        </div>
      </section>

      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-6">
        <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-6">
          <MetricCard label="총 처리 건수" value={formatCount(summary.processedCount)} />
          <MetricCard label="총 결제금액" value={formatWon(summary.paymentAmount)} />
          <MetricCard label="총 취소금액" value={formatWon(summary.cancelAmount)} />
          <MetricCard label="총 수수료" value={formatWon(summary.feeAmount)} />
          <MetricCard label="총 정산금액" value={formatWon(summary.settlementAmount)} strong />
          <MetricCard label="배치 처리 시간" value={formatMs(summary.elapsedMs)} accent />
        </section>

        <section className="grid gap-6 xl:grid-cols-[1fr_420px]">
          <DataPanel title="가맹점별 정산 결과">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-100 text-xs font-semibold uppercase text-muted">
                  <tr>
                    <Th>가맹점명</Th>
                    <Th align="right">총 결제금액</Th>
                    <Th align="right">총 취소금액</Th>
                    <Th align="right">순매출</Th>
                    <Th align="right">수수료율</Th>
                    <Th align="right">수수료</Th>
                    <Th align="right">최종 정산금액</Th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {settlementRows.map((row) => (
                    <tr key={row.merchantName} className="bg-white">
                      <Td strong>{row.merchantName}</Td>
                      <Td align="right">{formatWon(row.paymentAmount)}</Td>
                      <Td align="right">{formatWon(row.cancelAmount)}</Td>
                      <Td align="right">{formatWon(row.netSales)}</Td>
                      <Td align="right">{formatPercent(row.feeRate)}</Td>
                      <Td align="right">{formatWon(row.feeAmount)}</Td>
                      <Td align="right" strong>
                        {formatWon(row.settlementAmount)}
                      </Td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </DataPanel>

          <DataPanel title="전략별 처리 시간">
            <div className="space-y-4">
              {strategies.map((item) => {
                const elapsedMs = strategyProfiles[item].elapsedMs;
                const width = `${Math.max(12, (elapsedMs / strategyProfiles.BASIC_LOOP.elapsedMs) * 100)}%`;
                return (
                  <div key={item}>
                    <div className="mb-2 flex items-center justify-between gap-3 text-sm">
                      <span className="font-semibold text-ink">{item}</span>
                      <span className="tabular-nums text-muted">{formatMs(elapsedMs)}</span>
                    </div>
                    <div className="h-3 rounded-full bg-slate-100">
                      <div className="h-3 rounded-full bg-accent" style={{ width }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </DataPanel>
        </section>

        <DataPanel title="배치 실행 이력">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-slate-100 text-xs font-semibold uppercase text-muted">
                <tr>
                  <Th>실행일시</Th>
                  <Th>정산일자</Th>
                  <Th>처리 전략</Th>
                  <Th align="right">처리 건수</Th>
                  <Th align="right">성공 건수</Th>
                  <Th align="right">실패 건수</Th>
                  <Th align="right">실행 시간</Th>
                  <Th>상태</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {[lastRun, ...historyRows.filter((row) => row.strategy !== lastRun.strategy)].map((row) => (
                  <tr key={`${row.executedAt}-${row.strategy}`} className="bg-white">
                    <Td>{row.executedAt}</Td>
                    <Td>{row.settlementDate}</Td>
                    <Td strong>{row.strategy}</Td>
                    <Td align="right">{formatCount(row.processedCount)}</Td>
                    <Td align="right">{formatCount(row.successCount)}</Td>
                    <Td align="right">{formatCount(row.failureCount)}</Td>
                    <Td align="right" strong>
                      {formatMs(row.elapsedMs)}
                    </Td>
                    <Td>
                      <span className="rounded-full bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-700">
                        {row.status}
                      </span>
                    </Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </DataPanel>
      </div>
    </main>
  );
}

function MetricCard({ label, value, strong = false, accent = false }) {
  return (
    <article className="rounded-md border border-line bg-white p-4">
      <p className="text-xs font-semibold text-muted">{label}</p>
      <p className={`mt-3 text-xl font-semibold ${accent ? 'text-accent' : 'text-ink'} ${strong ? 'text-2xl' : ''}`}>
        {value}
      </p>
    </article>
  );
}

function DataPanel({ title, children }) {
  return (
    <section className="rounded-md border border-line bg-white">
      <div className="border-b border-line px-5 py-4">
        <h2 className="text-base font-semibold text-ink">{title}</h2>
      </div>
      <div className="p-5">{children}</div>
    </section>
  );
}

function Th({ children, align = 'left' }) {
  return <th className={`whitespace-nowrap px-4 py-3 ${align === 'right' ? 'text-right' : 'text-left'}`}>{children}</th>;
}

function Td({ children, align = 'left', strong = false }) {
  return (
    <td
      className={`whitespace-nowrap px-4 py-3 ${align === 'right' ? 'text-right tabular-nums' : 'text-left'} ${
        strong ? 'font-semibold text-ink' : 'text-slate-700'
      }`}
    >
      {children}
    </td>
  );
}

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
