import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext.js'
import { userApi } from '../api/userApi.js'

const PAGE_SIZE = 20
const EMPTY_FORM = {
  username: '',
  fullname: '',
  email: '',
  password: '',
  phone: '',
  role: 'STUDENT',
  universityId: '',
}

export default function AdminUsersPage() {
  const { authenticated, getToken, isAdmin } = useAuth()
  const [page, setPage] = useState(0)
  const [users, setUsers] = useState([])
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 })
  const [universities, setUniversities] = useState([])
  const [selected, setSelected] = useState(null)
  const [form, setForm] = useState(null)
  const [formMode, setFormMode] = useState(null)
  const [loadedPage, setLoadedPage] = useState(null)
  const [failedPage, setFailedPage] = useState(null)
  const [saving, setSaving] = useState(false)
  const [changingStatus, setChangingStatus] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const loading = loadedPage !== page && failedPage !== page

  const load = useCallback(async (pageNumber = page) => {
    const token = await getToken()
    if (!token) throw new Error('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.')
    const [result, schools] = await Promise.all([
      userApi.listUsers(token, pageNumber, PAGE_SIZE),
      userApi.universities(),
    ])
    return { result, schools }
  }, [getToken, page])

  useEffect(() => {
    if (!authenticated || !isAdmin) return
    let active = true
    load(page)
      .then(({ result, schools }) => {
        if (!active) return
        setUsers(result.content ?? [])
        setPageInfo({ totalPages: result.totalPages ?? 0, totalElements: result.totalElements ?? 0 })
        setUniversities(schools)
        setLoadedPage(page)
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

  function selectUser(user) {
    setSelected(user)
    setForm({
      fullname: user.fullname ?? '',
      phone: user.phone ?? '',
      role: user.role,
      universityId: user.universityId == null ? '' : String(user.universityId),
      enabled: user.enabled,
    })
    setFormMode('edit')
    setError('')
    setNotice('')
  }

  async function showDetails(user) {
    setError('')
    setNotice('')
    try {
      const token = await getToken()
      const details = await userApi.getAdminUser(token, user.username)
      setSelected(details)
      setForm(null)
      setFormMode('view')
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  function startCreate() {
    setSelected(null)
    setForm({ ...EMPTY_FORM })
    setFormMode('create')
    setError('')
    setNotice('')
  }

  function closeEditor() {
    setSelected(null)
    setForm(null)
    setFormMode(null)
  }

  async function changeEnabled(user) {
    const enabled = !user.enabled
    const action = enabled ? 'kích hoạt lại' : 'vô hiệu hoá'
    if (!enabled && !window.confirm(`Vô hiệu hoá tài khoản "${user.username}"? Tài khoản sẽ không thể đăng nhập.`)) {
      return
    }
    setChangingStatus(user.username)
    setError('')
    setNotice('')
    try {
      const token = await getToken()
      await userApi.setUserEnabled(token, user.username, enabled)
      const { result, schools } = await load(page)
      setUsers(result.content ?? [])
      setPageInfo({ totalPages: result.totalPages ?? 0, totalElements: result.totalElements ?? 0 })
      setUniversities(schools)
      setNotice(`Đã ${action} tài khoản ${user.username}.`)
      if (selected?.username === user.username) {
        setSelected((current) => current ? { ...current, enabled } : current)
      }
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setChangingStatus('')
    }
  }

  async function submit(event) {
    event.preventDefault()
    if (!form) return
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const token = await getToken()
      if (formMode === 'create') {
        await userApi.createAdminUser(token, {
          ...form,
          universityId: form.universityId ? Number(form.universityId) : null,
        })
        closeEditor()
        setNotice(`Đã tạo tài khoản ${form.username}.`)
        if (page !== 0) {
          setPage(0)
          return
        }
      } else if (formMode === 'edit' && selected) {
        await userApi.updateUser(token, selected.username, {
          fullname: form.fullname,
          phone: form.phone,
          role: form.role,
          universityId: form.universityId ? Number(form.universityId) : null,
          enabled: selected.enabled,
        })
        setNotice(`Đã cập nhật tài khoản ${selected.username}.`)
        closeEditor()
      } else {
        return
      }

      const { result, schools } = await load(page === 0 ? 0 : page)
      setUsers(result.content ?? [])
      setPageInfo({ totalPages: result.totalPages ?? 0, totalElements: result.totalElements ?? 0 })
      setUniversities(schools)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  if (!authenticated) {
    return <main className="user-page"><div className="user-alert">Vui lòng <a href="#/login">đăng nhập</a> bằng tài khoản quản trị.</div></main>
  }
  if (!isAdmin) {
    return <main className="user-page"><div className="user-alert user-alert--error" role="alert">Bạn không có quyền truy cập trang quản lý người dùng.</div></main>
  }

  return (
    <main className="user-page admin-page">
      <div className="page-title">
        <div><span className="eyebrow">QUẢN TRỊ HỆ THỐNG</span><h1>Quản lý người dùng</h1></div>
        <div className="user-actions">
          <div className="admin-count">{pageInfo.totalElements} tài khoản</div>
          <button className="button button--primary" type="button" onClick={startCreate}>+ Tạo tài khoản</button>
        </div>
      </div>
      {error && <div className="user-alert user-alert--error" role="alert">{error}</div>}
      {notice && <div className="user-alert user-alert--success" role="status">{notice}</div>}
      <section className="user-panel">
        {loading ? <p>Đang tải danh sách người dùng…</p> : (
          <>
            <div className="user-table-wrap">
              <table className="user-table">
                <thead><tr><th>Tài khoản</th><th>Vai trò</th><th>Trường</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                <tbody>
                  {users.map((user) => (
                    <tr key={user.username} className={selected?.username === user.username ? 'is-selected' : ''}>
                      <td><strong>{user.fullname}</strong><small>{user.username} · {user.email}</small></td>
                      <td><span className="role-pill">{user.role}</span></td>
                      <td>{user.universityName || '—'}</td>
                      <td><span className={`status-pill${user.deleted ? ' status-pill--deleted' : user.enabled ? ' status-pill--enabled' : ' status-pill--disabled'}`}>
                        {user.deleted ? 'Đã xoá mềm' : user.enabled ? 'Hoạt động' : 'Đã khoá'}
                      </span></td>
                      <td>
                        <div className="admin-row-actions">
                          <button className="button button--ghost button--small" type="button" onClick={() => showDetails(user)}>Chi tiết</button>
                          <button className="button button--ghost button--small" type="button" onClick={() => selectUser(user)}>Sửa</button>
                          <button
                            className={`button button--small ${user.enabled ? 'button--danger-ghost' : 'button--ghost'}`}
                            type="button"
                            disabled={changingStatus === user.username}
                            onClick={() => changeEnabled(user)}
                          >
                            {changingStatus === user.username ? 'Đang lưu…' : user.enabled ? 'Vô hiệu hoá' : 'Kích hoạt'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!users.length && <tr><td colSpan="5" className="user-table__empty">Chưa có người dùng nào.</td></tr>}
                </tbody>
              </table>
            </div>
            <div className="table-pagination">
              <span>Trang {page + 1} / {Math.max(pageInfo.totalPages, 1)}</span>
              <div>
                <button className="button button--ghost button--small" type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Trước</button>
                <button className="button button--ghost button--small" type="button" disabled={page + 1 >= pageInfo.totalPages} onClick={() => setPage((current) => current + 1)}>Sau</button>
              </div>
            </div>
          </>
        )}
      </section>

      {form && (
        <section className="user-panel admin-edit">
          <div className="section-heading">
            <div>
              <span className="eyebrow">{formMode === 'create' ? 'TẠO TÀI KHOẢN' : 'CẬP NHẬT TÀI KHOẢN'}</span>
              <h2>{formMode === 'create' ? 'Tài khoản mới' : selected?.username}</h2>
            </div>
            <button className="icon-button" type="button" aria-label="Đóng chỉnh sửa" onClick={closeEditor}>×</button>
          </div>
          <form className="user-form" onSubmit={submit}>
            <div className="user-form__grid">
              {formMode === 'create' && (
                <>
                  <label className="user-field"><span>Tên đăng nhập *</span><input value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} maxLength="20" required /></label>
                  <label className="user-field"><span>Email *</span><input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} maxLength="128" required /></label>
                  <label className="user-field"><span>Mật khẩu ban đầu *</span><input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} minLength="8" maxLength="72" required autoComplete="new-password" /></label>
                </>
              )}
              <label className="user-field"><span>Họ và tên *</span><input value={form.fullname} onChange={(event) => setForm({ ...form, fullname: event.target.value })} maxLength="128" required /></label>
              <label className="user-field"><span>Số điện thoại</span><input value={form.phone ?? ''} onChange={(event) => setForm({ ...form, phone: event.target.value })} maxLength="15" /></label>
              <label className="user-field">
                <span>Vai trò *</span>
                <select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value })}>
                  <option value="STUDENT">Học viên</option>
                  <option value="INSTRUCTOR">Giảng viên</option>
                  <option value="ADMINISTRATOR">Quản trị viên</option>
                </select>
              </label>
              <label className="user-field">
                <span>Trường đại học</span>
                <select value={form.universityId} onChange={(event) => setForm({ ...form, universityId: event.target.value })}>
                  <option value="">Không đổi / chưa chọn</option>
                  {universities.map((university) => <option key={university.id} value={university.id}>{university.name}</option>)}
                </select>
              </label>
            </div>
            <div className="user-actions">
              <button className="button button--primary" type="submit" disabled={saving}>{saving ? 'Đang lưu…' : formMode === 'create' ? 'Tạo tài khoản' : 'Lưu thay đổi'}</button>
              <button className="button button--ghost" type="button" onClick={closeEditor}>Huỷ</button>
            </div>
          </form>
        </section>
      )}

      {formMode === 'view' && selected && (
        <section className="user-panel admin-edit">
          <div className="section-heading">
            <div><span className="eyebrow">CHI TIẾT TÀI KHOẢN</span><h2>{selected.fullname}</h2></div>
            <button className="icon-button" type="button" aria-label="Đóng chi tiết" onClick={closeEditor}>×</button>
          </div>
          <dl className="admin-user-details">
            <div><dt>Tên đăng nhập</dt><dd>{selected.username}</dd></div>
            <div><dt>Email</dt><dd>{selected.email}</dd></div>
            <div><dt>Số điện thoại</dt><dd>{selected.phone || '—'}</dd></div>
            <div><dt>Vai trò</dt><dd>{selected.role}</dd></div>
            <div><dt>Trường đại học</dt><dd>{selected.universityName || '—'}</dd></div>
            <div><dt>Trạng thái</dt><dd>{selected.enabled ? 'Đang hoạt động' : 'Đã vô hiệu hoá'}</dd></div>
          </dl>
          <button className="button button--primary" type="button" onClick={() => selectUser(selected)}>Chỉnh sửa</button>
        </section>
      )}
    </main>
  )
}
