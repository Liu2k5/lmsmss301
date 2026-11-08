import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext.js'
import { userApi } from '../api/userApi.js'

const PAGE_SIZE = 20

export default function AdminRequestsPage() {
  const { authenticated, getToken, isAdmin } = useAuth()
  const [page, setPage] = useState(0)
  const [requests, setRequests] = useState([])
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 })
  const [notes, setNotes] = useState({})
  const [loadingPage, setLoadingPage] = useState(null)
  const [failedPage, setFailedPage] = useState(null)
  const [workingId, setWorkingId] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const loading = loadingPage !== page && failedPage !== page

  const load = useCallback(async (pageNumber) => {
    const token = await getToken()
    if (!token) throw new Error('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.')
    return userApi.listAccountRequests(token, pageNumber, PAGE_SIZE)
  }, [getToken])

  useEffect(() => {
    if (!authenticated || !isAdmin) return
    let active = true
    load(page)
      .then((result) => {
        if (!active) return
        setRequests(result.content ?? [])
        setPageInfo({ totalPages: result.totalPages ?? 0, totalElements: result.totalElements ?? 0 })
        setLoadingPage(page)
        setFailedPage(null)
        setError('')
      })
      .catch((requestError) => {
        if (!active) return
        setError(requestError.message)
        setFailedPage(page)
      })
    return () => { active = false }
  }, [authenticated, isAdmin, load, page])

  async function review(request, approved) {
    setWorkingId(request.id)
    setError('')
    setNotice('')
    try {
      const token = await getToken()
      await userApi.reviewAccountRequest(token, request.id, approved, notes[request.id] ?? '')
      const result = await load(page)
      setRequests(result.content ?? [])
      setPageInfo({ totalPages: result.totalPages ?? 0, totalElements: result.totalElements ?? 0 })
      setNotice(approved ? 'Đã duyệt yêu cầu.' : 'Đã từ chối yêu cầu.')
      setNotes((current) => {
        const next = { ...current }
        delete next[request.id]
        return next
      })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setWorkingId(null)
    }
  }

  if (!authenticated) {
    return <main className="user-page"><div className="user-alert">Vui lòng <a href="#/login">đăng nhập</a> bằng tài khoản quản trị.</div></main>
  }
  if (!isAdmin) {
    return <main className="user-page"><div className="user-alert user-alert--error" role="alert">Bạn không có quyền truy cập trang này.</div></main>
  }

  return (
    <main className="user-page">
      <div className="page-title">
        <div><span className="eyebrow">QUẢN TRỊ HỆ THỐNG</span><h1>Yêu cầu cần duyệt</h1></div>
        <div className="user-actions">
          <a className="button button--ghost" href="#/admin/users">Quản lý người dùng</a>
          <span className="admin-count">{pageInfo.totalElements} đang chờ</span>
        </div>
      </div>
      {error && <div className="user-alert user-alert--error" role="alert">{error}</div>}
      {notice && <div className="user-alert user-alert--success" role="status">{notice}</div>}
      {loading ? <section className="user-panel">Đang tải yêu cầu…</section> : (
        <section className="admin-request-list">
          {requests.map((request) => (
            <article className="user-panel admin-request-card" key={request.id}>
              <div className="admin-request-card__header">
                <div>
                  <span className={`request-type request-type--${request.type.toLowerCase()}`}>
                    {request.type === 'DELETION' ? 'Yêu cầu xoá tài khoản' : 'Xin chuyển sang giảng viên'}
                  </span>
                  <h2>{request.fullname || request.username}</h2>
                  <p>{request.username} · {request.email}</p>
                </div>
                <time dateTime={request.createdAt}>{new Date(request.createdAt).toLocaleString('vi-VN')}</time>
              </div>
              <label className="user-field">
                <span>Ghi chú cho người gửi (tuỳ chọn)</span>
                <textarea rows="2" maxLength="500" value={notes[request.id] ?? ''}
                  onChange={(event) => setNotes((current) => ({ ...current, [request.id]: event.target.value }))} />
              </label>
              <div className="user-actions admin-request-card__actions">
                <button className="button button--primary" type="button" disabled={workingId === request.id}
                  onClick={() => review(request, true)}>
                  {workingId === request.id ? 'Đang xử lý…' : 'Duyệt'}
                </button>
                <button className="button button--danger-ghost" type="button" disabled={workingId === request.id}
                  onClick={() => review(request, false)}>
                  Từ chối
                </button>
              </div>
            </article>
          ))}
          {!requests.length && <div className="user-panel user-table__empty">Không có yêu cầu nào đang chờ duyệt.</div>}
          {pageInfo.totalPages > 1 && (
            <div className="table-pagination">
              <span>Trang {page + 1} / {pageInfo.totalPages}</span>
              <div>
                <button className="button button--ghost button--small" type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Trước</button>
                <button className="button button--ghost button--small" type="button" disabled={page + 1 >= pageInfo.totalPages} onClick={() => setPage((current) => current + 1)}>Sau</button>
              </div>
            </div>
          )}
        </section>
      )}
    </main>
  )
}
