const ADMIN_ONLY_ROUTE_PREFIXES = [
	'/analytics',
	'/buildings',
	'/rooms',
	'/instructors',
	'/courses',
	'/timeslots',
	'/solver',
]

// Exact paths that only admins may visit.
const ADMIN_ONLY_EXACT_ROUTES = ['/schedules/new', '/requests/admin']

export function isAdminOnlyPath(path: string): boolean {
	if (ADMIN_ONLY_EXACT_ROUTES.includes(path)) return true
	return ADMIN_ONLY_ROUTE_PREFIXES.some(prefix => path === prefix || path.startsWith(`${prefix}/`))
}

export function isStudentAllowedPath(path: string): boolean {
	return path === '/'
}
