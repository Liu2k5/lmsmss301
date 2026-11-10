import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext.js'
import { reviewApi } from '../api/reviewApi.js'

const PAGE_SIZE = 10
const REPORT_STATUS_TABS = [
  { value: 'PENDING', label: 'Chờ xử lý' },
  { value: 'RESOLVED', label: 'Đã giải quyết' },
  { value: 'DISMISSED', label: 'Đã bác bỏ' },
]

export default function AdminReviewsPage() {
  const { authenticated, isAdmin } = useAuth()
  const [activeTab, setActiveTab] = useState('PENDING')
  const [page, setPage] = useState(0)
  const [reports, setReports] = useState([])
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [actionInProgress, setActionInProgress] = useState(null)

  const loadReports = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const result = await reviewApi.adminReports({
        status: activeTab,
        page,
        size: PAGE_SIZE,
      })
      setReports(result.content ?? [])
      setPageInfo({
        totalPages: result.totalPages ?? 0,
        totalElements: result.totalElements ?? 0,
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [activeTab, page])

  useEffect(() => {
    if (!authenticated || !isAdmin) return
    loadReports()
  }, [authenticated, isAdmin, loadReports])

  useEffect(() => {
    setPage(0)
  }, [activeTab])

  async function handleUpdateReviewStatus(report, status) {
    setActionInProgress(`status-${report.id}`)
    setError('')
    setNotice('')
    try {
      await reviewApi.adminUpdateStatus(report.reviewId, status)
      setNotice(status === 'HIDDEN' ? `Đã ẩn nhận xét #${report.reviewId}.` : `Đã duyệt nhận xét #${report.reviewId}.`)
      await loadReports()
    } catch (err) {
      setError(err.message)
    } finally {
      setActionInProgress(null)
    }
  }

  async function handleUpdateReportStatus(reportId, status) {
    setActionInProgress(`report-${reportId}`)
    setError('')
    setNotice('')
    try {
      await reviewApi.adminUpdateReportStatus(reportId, status)
      setNotice(status === 'RESOLVED' ? 'Đã đánh dấu báo cáo là ĐÃ GIẢI QUYẾT.' : 'Đã bác bỏ báo cáo.')
      await loadReports()
    } catch (err) {
      setError(err.message)
    } finally {
      setActionInProgress(null)
    }
  }

  if (!authenticated) {
    return (
      <main className="user-page">
        <div className="user-alert">
          Vui lòng <a href="#/login">đăng nhập</a> bằng tài khoản quản trị.
        </div>
      </main>
    )
  }

  if (!isAdmin) {
    return (
      <main className="user-page">
        <div className="user-alert user-alert--error" role="alert">
          Bạn không có quyền truy cập trang này.
        </div>
      </main>
    )
  }

  return (
    <main className="user-page">
      <div className="page-title">
        <div>
          <span className="eyebrow">QUẢN TRỊ HỆ THỐNG</span>
          <h1>Kiểm duyệt Đánh giá & Nhận xét</h1>
        </div>
        <div className="user-actions">
          <a className="button button--ghost" href="#/admin/users">
            Quản lý người dùng
          </a>
          <a className="button button--ghost" href="#/admin/requests">
            Yêu cầu tài khoản
          </a>
          <span className="admin-count">{pageInfo.totalElements} báo cáo</span>
        </div>
      </div>

      <div className="admin-tabs" role="tablist">
        {REPORT_STATUS_TABS.map((tab) => (
          <button
            key={tab.value}
            type="button"
            className={`admin-tab${activeTab === tab.value ? ' is-active' : ''}`}
            onClick={() => setActiveTab(tab.value)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && (
        <div className="user-alert user-alert--error" role="alert">
          {error}
        </div>
      )}
      {notice && (
        <div className="user-alert user-alert--success" role="status">
          {notice}
        </div>
      )}

      {loading ? (
        <section className="user-panel">Đang tải danh sách báo cáo vi phạm…</section>
      ) : (
        <section className="admin-request-list">
          {reports.map((report) => (
            <article className="user-panel admin-request-card" key={report.id}>
              <div className="admin-request-card__header">
                <div>
                  <span className={`request-type request-type--${report.status.toLowerCase()}`}>
                    Báo cáo vi phạm #{report.id}
                  </span>
                  <h2>Nhận xét #{report.reviewId}</h2>
                  <p>
                    Người báo cáo: <code>{report.reporterId}</code>
                  </p>
                </div>
                <time dateTime={report.createdAt}>
                  {new Date(report.createdAt).toLocaleString('vi-VN')}
                </time>
              </div>

              <div className="admin-report__reason">
                <strong>Lý do báo cáo:</strong>
                <p className="admin-report__reason-text">{report.reason}</p>
              </div>

              <div className="user-actions admin-request-card__actions">
                <button
                  type="button"
                  className="button button--danger"
                  disabled={actionInProgress != null}
                  onClick={() => handleUpdateReviewStatus(report, 'HIDDEN')}
                >
                  {actionInProgress === `status-${report.id}` ? 'Đang xử lý…' : '🚫 Ẩn nhận xét vi phạm'}
                </button>

                <button
                  type="button"
                  className="button button--ghost"
                  disabled={actionInProgress != null}
                  onClick={() => handleUpdateReviewStatus(report, 'PUBLISHED')}
                >
                  ✓ Duyệt hiển thị nhận xét
                </button>

                {report.status === 'PENDING' && (
                  <>
                    <button
                      type="button"
                      className="button button--primary"
                      disabled={actionInProgress != null}
                      onClick={() => handleUpdateReportStatus(report.id, 'RESOLVED')}
                    >
                      Đã giải quyết
                    </button>
                    <button
                      type="button"
                      className="button button--ghost"
                      disabled={actionInProgress != null}
                      onClick={() => handleUpdateReportStatus(report.id, 'DISMISSED')}
                    >
                      Bác bỏ báo cáo
                    </button>
                  </>
                )}
              </div>
            </article>
          ))}

          {!reports.length && (
            <div className="user-panel user-table__empty">
              Không có báo cáo vi phạm nào trong mục này.
            </div>
          )}

          {pageInfo.totalPages > 1 && (
            <div className="table-pagination">
              <span>
                Trang {page + 1} / {pageInfo.totalPages}
              </span>
              <div>
                <button
                  className="button button--ghost button--small"
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((current) => current - 1)}
                >
                  Trước
                </button>
                <button
                  className="button button--ghost button--small"
                  type="button"
                  disabled={page + 1 >= pageInfo.totalPages}
                  onClick={() => setPage((current) => current + 1)}
                >
                  Sau
                </button>
              </div>
            </div>
          )}
        </section>
      )}
    </main>
  )
}
