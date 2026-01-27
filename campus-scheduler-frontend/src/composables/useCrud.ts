import { ref, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import type { AxiosError } from 'axios'
import { toast } from 'vue3-toastify'

export interface UseCrudOptions<T, TCreate, TUpdate = Partial<TCreate>> {
    /** Service methods */
    getAll: () => Promise<T[]>
    getById?: (id: number) => Promise<T>
    create?: (data: TCreate, ...args: unknown[]) => Promise<T>
    update?: (id: number, data: TUpdate) => Promise<T>
    deleteItem?: (id: number) => Promise<void>
    /** Route to navigate after create/update */
    listRoute: string
    /** Confirmation message for delete */
    deleteConfirm?: string
}

export interface UseCrudReturn<T> {
    items: Ref<T[]>
    loading: Ref<boolean>
    saving: Ref<boolean>
    error: Ref<string | null>
    fetchAll: () => Promise<void>
    handleDelete: (id: number) => Promise<void>
    handleSave: <TData>(data: TData, id?: number, ...args: unknown[]) => Promise<void>
}

export function useCrud<T extends { id: number }, TCreate, TUpdate = Partial<TCreate>>(
    options: UseCrudOptions<T, TCreate, TUpdate>
): UseCrudReturn<T> {
    const router = useRouter()
    const items = ref<T[]>([]) as Ref<T[]>
    const loading = ref(false)
    const saving = ref(false)
    const error = ref<string | null>(null)

    async function fetchAll() {
        loading.value = true
        error.value = null
        try {
            items.value = await options.getAll()
        } catch (e) {
            error.value = 'Failed to load data'
            console.error(e)
        } finally {
            loading.value = false
        }
    }

    async function handleDelete(id: number) {
        if (!options.deleteItem) return
        const msg = options.deleteConfirm || 'Are you sure you want to delete this item?'
        if (!confirm(msg)) return

        try {
            await options.deleteItem(id)
            items.value = items.value.filter(item => item.id !== id)
            toast.success('Deleted successfully')
        } catch (e) {
            const axiosError = e as AxiosError<{ message?: string }>
            const errorMsg = axiosError.response?.data?.message || 'Failed to delete item'
            toast.error(errorMsg)
            console.error(e)
        }
    }

    async function handleSave<TData>(data: TData, id?: number, ...args: unknown[]) {
        saving.value = true
        error.value = null

        try {
            if (id !== undefined && options.update) {
                await options.update(id, data as unknown as TUpdate)
                toast.success('Updated successfully')
            } else if (options.create) {
                await options.create(data as unknown as TCreate, ...args)
                toast.success('Created successfully')
            }
            router.push(options.listRoute)
        } catch (e) {
            const axiosError = e as AxiosError<{ message?: string }>
            error.value = axiosError.response?.data?.message || 'Failed to save'
            toast.error(error.value)
            console.error(e)
        } finally {
            saving.value = false
        }
    }

    return { items, loading, saving, error, fetchAll, handleDelete, handleSave }
}
