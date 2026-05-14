import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import axios from 'axios';
import './styles.css';

const API_BASE_URL = 'http://localhost:8080/api';
const strategyOptions = [
  { value: 'BASIC_LOOP', label: 'BASIC_LOOP', implemented: true },
  { value: 'GROUP_BY_QUERY', label: 'GROUP_BY_QUERY', implemented: true },
  { value: 'GROUP_BY_BULK_SAVE', label: 'GROUP_BY_BULK_SAVE - 아직 미구현', implemented: false },
  { value: 'GROUP_BY_BULK_INDEX', label: 'GROUP_BY_BULK_INDEX - 아직 미구현', implemented: false },
];

const emptySummary = {
  processedCount: 0,
  totalPaymentAmount: 0,
  totalCancelAmount: 0,
  totalFeeAmount: 0,
  totalSettlementAmount: 0,
  elapsedMs: 0,
  settlements: [],
};

const toNumber = (value) => Number(value ?? 0);

const formatWon = (value) =>
  new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(toNumber(value));

const formatCount = (value) => `${new Intl.NumberFormat('ko-KR').format(toNumber(value))}건`;
const formatMs = (value) => `${new Intl.NumberFormat('ko-KR').format(toNumber(value))}ms`;
const formatPercent = (value) => `${(toNumber(value) * 100).toFixed(2)}%`;

function App() {
  const [settlementDate, setSettlementDate] = useState('2026-05-08');
  const [strategy, setStrategy] = useState('BASIC_LOOP');
  const [summary, setSummary] = useState(emptySummary);
  const [histories, setHistories] = useState([]);
  const [isRunning, setIsRunning] = useState(false);
  const [isResetting, setIsResetting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const lastRun = histories[0];
  const selectedStrategy = strategyOptions.find((item) => item.value === strategy);
  const isStrategyImplemented = selectedStrategy?.implemented ?? false;

  const fetchSettlements = async (date, selectedStrategy) => {
    const response = await axios.get(`${API_BASE_URL}/settlements`, {
      params: { date, strategy: selectedStrategy },
    });
    setSummary(response.data);
  };

  const fetchHistories = async () => {
    const response = await axios.get(`${API_BASE_URL}/batch-histories`);
    setHistories(response.data);
  };

  const refreshDashboard = async (date, selectedStrategy) => {
    await Promise.all([fetchSettlements(date, selectedStrategy), fetchHistories()]);
  };

  useEffect(() => {
    refreshDashboard(settlementDate, strategy).catch(() => {
      setErrorMessage('대시보드 데이터를 불러오지 못했습니다. 백엔드 서버 상태를 확인하세요.');
    });
  }, [settlementDate, strategy]);

  const handleRun = async () => {
    setIsRunning(true);
    setErrorMessage('');

    if (!isStrategyImplemented) {
      setErrorMessage(`${strategy} 전략은 아직 구현되지 않았습니다.`);
      setIsRunning(false);
      return;
    }

    try {
      const response = await axios.post(`${API_BASE_URL}/settlements/run`, null, {
        params: {
          date: settlementDate,
          strategy,
        },
      });
      setSummary(response.data);
      await fetchHistories();
    } catch (error) {
      const message = error.response?.data?.message ?? '정산 배치 실행 중 오류가 발생했습니다.';
      setErrorMessage(message);
      await refreshDashboard(settlementDate, strategy).catch(() => undefined);
    } finally {
      setIsRunning(false);
    }
  };

  const handleReset = async () => {
    setIsResetting(true);
    setErrorMessage('');

    try {
      const response = await axios.delete(`${API_BASE_URL}/settlements`, {
        params: {
          date: settlementDate,
        },
      });
      setSummary(emptySummary);
      await fetchHistories();
      setErrorMessage(`${settlementDate} 정산 결과 ${formatCount(response.data.deletedCount)}를 초기화했습니다.`);
    } catch (error) {
      const message = error.response?.data?.message ?? '정산 결과 초기화 중 오류가 발생했습니다.';
      setErrorMessage(message);
      await refreshDashboard(settlementDate, strategy).catch(() => undefined);
    } finally {
      setIsResetting(false);
    }
  };

  const elapsedRows = useMemo(
    () => histories.filter((history) => history.strategy === strategy).slice(0, 5),
    [histories, strategy],
  );

  return (
    <main className="min-h-screen bg-panel text-ink">
      <section className="border-b border-line bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-6">
          <div className="flex flex-col gap-2">
            <p className="text-sm font-semibold text-accent">Settlement Performance</p>
            <h1 className="text-2xl font-semibold tracking-normal text-ink">정산 성능 대시보드</h1>
            <p className="max-w-3xl text-sm leading-6 text-muted">
              BASIC_LOOP 기준선과 단계형 개선 전략별 정산 결과, 실행 이력, 처리 시간을 비교합니다.
            </p>
          </div>

          <div className="grid gap-3 rounded-md border border-line bg-panel p-4 lg:grid-cols-[180px_220px_1fr_auto_auto]">
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
                {strategyOptions.map((item) => (
                  <option key={item.value} value={item.value} disabled={!item.implemented}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>

            <div className="flex items-end">
              <div className="w-full rounded-md border border-line bg-white px-4 py-3">
                <p className="text-xs font-semibold text-muted">최근 실행</p>
                <p className="mt-1 text-sm font-semibold text-ink">
                  {lastRun
                    ? `${lastRun.strategy} · ${formatCount(lastRun.processedCount)} · ${formatMs(lastRun.elapsedMs)}`
                    : '아직 실행 이력이 없습니다.'}
                </p>
              </div>
            </div>

            <button
              className="h-11 self-end rounded-md bg-accent px-5 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-slate-400"
              type="button"
              onClick={handleRun}
              disabled={isRunning || isResetting || !isStrategyImplemented}
            >
              {isRunning ? '정산 실행 중...' : '정산 배치 실행'}
            </button>

            <button
              className="h-11 self-end rounded-md border border-line bg-white px-5 text-sm font-semibold text-ink hover:border-rose-300 hover:text-rose-700 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
              type="button"
              onClick={handleReset}
              disabled={isRunning || isResetting}
            >
              {isResetting ? '초기화 중...' : '정산 결과 초기화'}
            </button>
          </div>

          {errorMessage && (
            <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-warning">
              {errorMessage}
            </div>
          )}
        </div>
      </section>

      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-6">
        <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-6">
          <MetricCard label="총 처리 건수" value={formatCount(summary.processedCount)} />
          <MetricCard label="총 결제금액" value={formatWon(summary.totalPaymentAmount)} />
          <MetricCard label="총 취소금액" value={formatWon(summary.totalCancelAmount)} />
          <MetricCard label="총 수수료" value={formatWon(summary.totalFeeAmount)} />
          <MetricCard label="총 정산금액" value={formatWon(summary.totalSettlementAmount)} strong />
          <MetricCard label={`${strategy} 처리 시간`} value={formatMs(summary.elapsedMs)} accent />
        </section>

        <section className="grid gap-6 xl:grid-cols-[1fr_420px]">
          <DataPanel title="가맹점별 정산 결과">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-100 text-xs font-semibold uppercase text-muted">
                  <tr>
                    <Th>가맹점명</Th>
                    <Th>처리 전략</Th>
                    <Th align="right">총 결제금액</Th>
                    <Th align="right">총 취소금액</Th>
                    <Th align="right">순매출</Th>
                    <Th align="right">수수료율</Th>
                    <Th align="right">수수료</Th>
                    <Th align="right">최종 정산금액</Th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {summary.settlements.length === 0 ? (
                    <tr className="bg-white">
                      <td className="px-4 py-8 text-center text-muted" colSpan="8">
                        선택한 날짜의 정산 결과가 없습니다.
                      </td>
                    </tr>
                  ) : (
                    summary.settlements.map((row) => (
                      <tr key={row.id} className="bg-white">
                        <Td strong>{row.merchantName}</Td>
                        <Td>{row.strategy}</Td>
                        <Td align="right">{formatWon(row.totalPaymentAmount)}</Td>
                        <Td align="right">{formatWon(row.totalCancelAmount)}</Td>
                        <Td align="right">{formatWon(row.netSalesAmount)}</Td>
                        <Td align="right">{formatPercent(row.feeRate)}</Td>
                        <Td align="right">{formatWon(row.feeAmount)}</Td>
                        <Td align="right" strong>
                          {formatWon(row.finalSettlementAmount)}
                        </Td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </DataPanel>

          <DataPanel title={`${strategy} 처리 시간`}>
            {elapsedRows.length === 0 ? (
              <p className="text-sm text-muted">아직 선택한 전략의 실행 이력이 없습니다.</p>
            ) : (
              <div className="space-y-4">
                {elapsedRows.map((item) => {
                  const maxElapsedMs = Math.max(...elapsedRows.map((history) => history.elapsedMs));
                  const width = `${Math.max(12, (item.elapsedMs / maxElapsedMs) * 100)}%`;
                  return (
                    <div key={item.id}>
                      <div className="mb-2 flex items-center justify-between gap-3 text-sm">
                        <span className="font-semibold text-ink">{formatDateTime(item.startedAt)}</span>
                        <span className="tabular-nums text-muted">{formatMs(item.elapsedMs)}</span>
                      </div>
                      <div className="h-3 rounded-full bg-slate-100">
                        <div className="h-3 rounded-full bg-accent" style={{ width }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
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
                  <Th>실패 원인</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {histories.length === 0 ? (
                  <tr className="bg-white">
                    <td className="px-4 py-8 text-center text-muted" colSpan="9">
                      배치 실행 이력이 없습니다.
                    </td>
                  </tr>
                ) : (
                  histories.map((row) => (
                    <tr key={row.id} className="bg-white">
                      <Td>{formatDateTime(row.startedAt)}</Td>
                      <Td>{row.settlementDate}</Td>
                      <Td strong>{row.strategy}</Td>
                      <Td align="right">{formatCount(row.processedCount)}</Td>
                      <Td align="right">{formatCount(row.successCount)}</Td>
                      <Td align="right">{formatCount(row.failureCount)}</Td>
                      <Td align="right" strong>
                        {formatMs(row.elapsedMs)}
                      </Td>
                      <Td>
                        <span className={`rounded-full px-2 py-1 text-xs font-semibold ${statusClassName(row.status)}`}>
                          {row.status}
                        </span>
                      </Td>
                      <Td>{row.errorMessage ?? '-'}</Td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </DataPanel>
      </div>
    </main>
  );
}

function statusClassName(status) {
  if (status === 'SUCCESS') {
    return 'bg-emerald-50 text-emerald-700';
  }
  if (status === 'FAILED') {
    return 'bg-rose-50 text-rose-700';
  }
  return 'bg-sky-50 text-sky-700';
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  return value.replace('T', ' ').slice(0, 19);
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
