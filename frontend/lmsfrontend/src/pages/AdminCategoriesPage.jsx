import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext.js'
import { categoryApi } from '../api/courseApi.js'
import ConfirmDialog from '../components/course/ConfirmDialog.jsx'

const EMPTY_FORM = { name: '', description: '' }

/**
 * Quản lý danh mục khoá học (UC-C25…C27) — chỉ Admin.
 * Admin là người quản lý hệ thống: tạo / sửa / xoá danh mục; giảng viên chọn danh mục khi soạn khoá học.
 */
export default function AdminCategoriesPage() {
  const { authenticated, isAdmin } = useAuth()
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  // formMode: null (đóng) | 'create' | 'edit'
  const [formMode, setFormMode] = useState(null)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setCategories(await categoryApi.list())
      setError('')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!authenticated || !isAdmin) return
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi vào trang
    load()
  }, [authenticated, isAdmin, load])

  function startCreate() {
    setFormMode('create')
    setEditing(null)
    setForm(EMPTY_FORM)
    setError('')
    setNotice('')
  }

  function startEdit(category) {
    setFormMode('edit')
    setEditing(category)
    setForm({ name: category.name, description: category.description ?? '' })
    setError('')
    setNotice('')
  }

  function closeForm() {
    setFormMode(null)
    setEditing(null)
    setForm(EMPTY_FORM)
  }

  async function submit(event) {
    event.preventDefault()
    const name = form.name.trim()
    if (!name) {
      setError('Tên danh mục không được để trống.')
      return
    }
    const payload = { name, description: form.description.trim() || null }

    setSaving(true)
    setError('')
    setNotice('')
    try {
      if (formMode === 'create') {
        await categoryApi.create(payload)
        setNotice(`Đã tạo danh mục "${name}".`)
      } else {
        await categoryApi.update(editing.id, payload)
        setNotice(`Đã cập nhật danh mục "${name}".`)
      }
      closeForm()
      await load()
    } catch (requestError) {
      // 409: trùng tên (không phân biệt hoa thường)
      setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  async function remove(category) {
    setConfirmDelete(null)
    setDeletingId(category.id)
    setError('')
    setNotice('')
    try {
      await categoryApi.remove(category.id)
      setNotice(`Đã xoá danh mục "${category.name}".`)
      if (editing?.id === category.id) closeForm()
      await load()
    } catch (requestError) {
      // 409: danh mục còn khoá học
      setError(requestError.message)
    } finally {
      setDeletingId(null)
    }
  }

  if (!authenticated) {
    return <main className="user-page"><div className="user-alert">Vui lòng <a href="#/login">đăng nhập</a> bằng tài khoản quản trị.</div></main>
  }
  if (!isAdmin) {
    return <main className="user-page"><div className="user-alert user-alert--error" role="alert">Bạn không có quyền truy cập trang quản lý danh mục.</div></main>
  }

  return (
    <main className="user-page">
      <div className="page-title">
        <div><span className="eyebrow">QUẢN TRỊ HỆ THỐNG</span><h1>Danh mục khoá học</h1></div>
        <div className="user-actions">
          <div className="admin-count">{categories.length} danh mục</div>
          <button className="button button--primary" type="button" onClick={startCreate}>+ Thêm danh mục</button>
        </div>
      </div>
      {error && <div className="user-alert user-alert--error" role="alert">{error}</div>}
      {notice && <div className="user-alert user-alert--success" role="status">{notice}</div>}

      {formMode && (
        <section className="user-panel admin-edit">
          <div className="section-heading">
            <div>
              <span className="eyebrow">{formMode === 'create' ? 'THÊM DANH MỤC' : 'SỬA DANH MỤC'}</span>
              <h2>{formMode === 'create' ? 'Danh mục mới' : editing?.name}</h2>
            </div>
            <button className="icon-button" type="button" aria-label="Đóng" onClick={closeForm}>×</button>
          </div>
          <form className="user-form" onSubmit={submit}>
            <div className="user-form__grid">
              <label className="user-field">
                <span>Tên danh mục *</span>
                <input value={form.name} maxLength="100" required autoFocus
                  onChange={(event) => setForm({ ...form, name: event.target.value })} />
              </label>
              <label className="user-field">
                <span>Mô tả</span>
                <textarea rows="2" maxLength="2000" value={form.description}
                  onChange={(event) => setForm({ ...form, description: event.target.value })} />
              </label>
            </div>
            <div className="user-actions">
              <button className="button button--primary" type="submit" disabled={saving}>
                {saving ? 'Đang lưu…' : formMode === 'create' ? 'Tạo danh mục' : 'Lưu thay đổi'}
              </button>
              <button className="button button--ghost" type="button" onClick={closeForm}>Huỷ</button>
            </div>
          </form>
        </section>
      )}

      <section className="user-panel">
        {loading ? <p>Đang tải danh mục…</p> : (
          <div className="user-table-wrap">
            <table className="user-table">
              <thead><tr><th>Danh mục</th><th>Số khoá học</th><th>Cập nhật</th><th>Thao tác</th></tr></thead>
              <tbody>
                {categories.map((category) => (
                  <tr key={category.id} className={editing?.id === category.id ? 'is-selected' : ''}>
                    <td><strong>{category.name}</strong><small>{category.description || '—'}</small></td>
                    <td>{category.courseCount}</td>
                    <td>{new Date(category.updatedAt).toLocaleString('vi-VN')}</td>
                    <td>
                      <div className="admin-row-actions">
                        <button className="button button--ghost button--small" type="button"
                          onClick={() => startEdit(category)}>
                          Sửa
                        </button>
                        <button className="button button--danger-ghost button--small" type="button"
                          disabled={deletingId === category.id || category.courseCount > 0}
                          title={category.courseCount > 0 ? 'Danh mục còn khoá học nên không xoá được' : undefined}
                          onClick={() => setConfirmDelete(category)}>
                          {deletingId === category.id ? 'Đang xoá…' : 'Xoá'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!categories.length && (
                  <tr><td colSpan="4" className="user-table__empty">Chưa có danh mục nào. Bấm “Thêm danh mục” để tạo.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {confirmDelete && (
        <ConfirmDialog
          title="Xoá danh mục?"
          message={`Danh mục “${confirmDelete.name}” sẽ bị xoá vĩnh viễn.`}
          confirmLabel="Xoá danh mục"
          onConfirm={() => remove(confirmDelete)}
          onCancel={() => setConfirmDelete(null)}
        />
      )}
    </main>
  )
}
