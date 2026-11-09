const API = 'http://localhost:8080/api/v1'

let authHeader = null
let currentPage = 0
const pageSize = 20
let lastPageData = null

const $ = id => document.getElementById(id)

function log(data) {
	$('apiLog').textContent =
		typeof data === 'string' ? data : JSON.stringify(data, null, 2)
}

function setStatus(connected) {
	const el = $('connectionStatus')
	el.textContent = connected ? 'Подключено' : 'Не подключено'
	el.classList.toggle('connected', connected)
}

function escapeText(value) {
	return value == null ? '' : String(value)
}

async function api(path, options = {}) {
	if (!authHeader) {
		throw new Error('Сначала авторизуйтесь')
	}

	const response = await fetch(`${API}${path}`, {
		...options,
		headers: {
			Authorization: authHeader,
			'Content-Type': 'application/json',
			...options.headers
		}
	})

	const raw = await response.text()
	let data

	try {
		data = raw ? JSON.parse(raw) : null
	} catch {
		data = raw
	}

	if (!response.ok) {
		const message =
			typeof data === 'object' && data !== null
				? data.message || data.error || JSON.stringify(data)
				: data

		throw new Error(`HTTP ${response.status}: ${message}`)
	}

	log({
		status: response.status,
		response: data
	})

	return data
}

function showError(error) {
	log(`Ошибка: ${error.message}`)
}

function getId(item) {
	return item.id ?? item.caseId ?? item.userId ?? ''
}

function getDisplayName(item) {
	return (
		item.caseNumber ??
		item.username ??
		item.fullName ??
		item.name ??
		getId(item) ??
		'Без названия'
	)
}

// Авторизация
$('loginBtn').addEventListener('click', async () => {
	const username = $('username').value.trim()
	const password = $('password').value

	if (!username || !password) {
		$('authMessage').textContent = 'Введите логин и пароль'
		return
	}

	authHeader = 'Basic ' + btoa(`${username}:${password}`)

	try {
		// Проверяем авторизацию запросом, доступным пользователю.
		// Для supervisor используется список дел, для investigator — уведомления.
		if (username.startsWith('supervisor')) {
			await api('/cases')
		} else {
			await api('/notifications/unread-count')
		}

		setStatus(true)
		$('authMessage').textContent = `Авторизован: ${username}`
	} catch (error) {
		authHeader = null
		setStatus(false)
		$('authMessage').textContent = error.message
		showError(error)
	}
})

$('logoutBtn').addEventListener('click', () => {
	authHeader = null
	currentPage = 0
	setStatus(false)
	$('authMessage').textContent = 'Вы вышли из системы'
	$('cases').textContent = 'Не авторизовано'
	$('recipients').textContent = ''
	$('notifications').textContent = 'Не авторизовано'
	$('unreadCount').textContent = '—'
	log('Сессия очищена')
})

// Загрузка дел
$('loadCasesBtn').addEventListener('click', loadCases)

async function loadCases() {
	try {
		const data = await api('/cases')
		const cases = Array.isArray(data) ? data : (data.content ?? [])

		const container = $('cases')
		container.replaceChildren()

		if (!cases.length) {
			container.textContent = 'Дела не найдены'
			return
		}

		cases.forEach(item => {
			const id = getId(item)
			const card = document.createElement('div')
			card.className = 'item'

			const content = document.createElement('div')
			content.className = 'item-content'

			const title = document.createElement('strong')
			title.textContent = getDisplayName(item)

			const details = document.createElement('small')
			details.textContent = `ID: ${id}`

			content.append(title, details)

			const button = document.createElement('button')
			button.textContent = 'Выбрать'
			button.addEventListener('click', () => {
				$('caseId').value = id
				loadRecipients(id, getDisplayName(item))
			})

			card.append(content, button)
			container.append(card)
		})
	} catch (error) {
		showError(error)
	}
}

// Получатели указания
async function loadRecipients(caseId, caseName) {
	try {
		$('recipientSection').hidden = false
		$('selectedCase').textContent = `Выбрано дело: ${caseName}`

		const data = await api(
			`/cases/${encodeURIComponent(caseId)}/instruction-recipients`
		)

		const recipients = Array.isArray(data) ? data : (data.content ?? [])
		const container = $('recipients')
		container.replaceChildren()

		if (!recipients.length) {
			container.textContent = 'Нет доступных получателей'
			return
		}

		recipients.forEach(item => {
			const id = getId(item)
			const card = document.createElement('div')
			card.className = 'item'

			const content = document.createElement('div')
			content.className = 'item-content'

			const title = document.createElement('strong')
			title.textContent = getDisplayName(item)

			const details = document.createElement('small')
			details.textContent = `ID: ${id}`

			content.append(title, details)

			const button = document.createElement('button')
			button.textContent = 'Выбрать'
			button.addEventListener('click', () => {
				$('recipientId').value = id
			})

			card.append(content, button)
			container.append(card)
		})
	} catch (error) {
		showError(error)
	}
}

// Создание указания
$('instructionForm').addEventListener('submit', async event => {
	event.preventDefault()

	const body = {
		caseId: $('caseId').value.trim(),
		recipientId: $('recipientId').value.trim(),
		text: $('instructionText').value.trim()
	}

	try {
		const result = await api('/instructions', {
			method: 'POST',
			body: JSON.stringify(body)
		})

		$('instructionResult').textContent = JSON.stringify(result, null, 2)

		$('instructionText').value = ''
	} catch (error) {
		$('instructionResult').textContent = error.message
		showError(error)
	}
})

// Уведомления
$('loadNotificationsBtn').addEventListener('click', async () => {
	currentPage = 0
	await loadNotifications()
})

async function loadNotifications() {
	try {
		const data = await api(
			`/notifications?page=${currentPage}&size=${pageSize}`
		)

		lastPageData = data
		const notifications = Array.isArray(data) ? data : (data.content ?? [])

		const container = $('notifications')
		container.replaceChildren()

		if (!notifications.length) {
			container.textContent = 'Уведомлений нет'
		}

		notifications.forEach(item => {
			const id = getId(item)
			const card = document.createElement('div')
			card.className = 'item'

			const content = document.createElement('div')
			content.className = 'item-content'

			const title = document.createElement('strong')
			title.textContent = `Уведомление ${id}`

			const details = document.createElement('small')
			details.textContent = JSON.stringify(item)

			content.append(title, details)
			card.append(content)

			if (!item.readAt) {
				const button = document.createElement('button')
				button.textContent = 'Прочитано'
				button.addEventListener('click', () => markAsRead(id))
				card.append(button)
			} else {
				const readLabel = document.createElement('small')
				readLabel.textContent = 'Прочитано'
				card.append(readLabel)
			}

			container.append(card)
		})

		$('pageInfo').textContent = `Страница ${currentPage + 1}`

		$('prevPageBtn').disabled = data.first === true || currentPage === 0

		$('nextPageBtn').disabled =
			data.last === true ||
			data.hasNext === false ||
			(!data.hasNext && notifications.length < pageSize)

		await loadUnreadCount()
	} catch (error) {
		showError(error)
	}
}

async function loadUnreadCount() {
	try {
		const data = await api('/notifications/unread-count')

		const count =
			typeof data === 'number'
				? data
				: (data.unreadCount ?? data.count ?? data.total ?? 0)

		$('unreadCount').textContent = count
	} catch (error) {
		$('unreadCount').textContent = 'Ошибка'
		showError(error)
	}
}

async function markAsRead(id) {
	try {
		await api(`/notifications/${encodeURIComponent(id)}/read`, {
			method: 'PATCH'
		})

		await loadNotifications()
	} catch (error) {
		showError(error)
	}
}

$('prevPageBtn').addEventListener('click', async () => {
	if (currentPage > 0) {
		currentPage--
		await loadNotifications()
	}
})

$('nextPageBtn').addEventListener('click', async () => {
	if (lastPageData?.last !== true && lastPageData?.hasNext !== false) {
		currentPage++
		await loadNotifications()
	}
})
