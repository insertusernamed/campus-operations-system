import { ref, type Ref } from 'vue'
import type { AxiosError } from 'axios'

export interface UseAsyncDataOptions<T> {
    immediate?: boolean
    onSuccess?: (data: T) => void
    onError?: (error: Error) => void
}

export interface UseAsyncDataReturn<T> {
    data: Ref<T | null>
    loading: Ref<boolean>
    error: Ref<string | null>
    execute: () => Promise<void>
    reset: () => void
}

export function useAsyncData<T>(
    fetcher: () => Promise<T>,
    options: UseAsyncDataOptions<T> = {}
): UseAsyncDataReturn<T> {
    const data = ref<T | null>(null) as Ref<T | null>
    const loading = ref(false)
    const error = ref<string | null>(null)

    async function execute() {
        loading.value = true
        error.value = null

        try {
            data.value = await fetcher()
            options.onSuccess?.(data.value)
        } catch (e) {
            const axiosError = e as AxiosError<{ message?: string }>
            if (axiosError.response?.status === 404) {
                error.value = 'Not found'
            } else {
                error.value = axiosError.response?.data?.message || 'An error occurred'
            }
            options.onError?.(e as Error)
            console.error(e)
        } finally {
            loading.value = false
        }
    }

    function reset() {
        data.value = null
        loading.value = false
        error.value = null
    }

    if (options.immediate !== false) {
        execute()
    }

    return { data, loading, error, execute, reset }
}
