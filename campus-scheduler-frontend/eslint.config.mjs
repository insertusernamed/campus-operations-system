import vue from 'eslint-plugin-vue'
import vueA11y from 'eslint-plugin-vuejs-accessibility'
import tsParser from '@typescript-eslint/parser'
import vueParser from 'vue-eslint-parser'

const a11yRules = {
	...vueA11y.configs.recommended.rules,
	'vuejs-accessibility/no-aria-hidden-on-focusable': 'error',
	'vuejs-accessibility/no-role-presentation-on-focusable': 'error',
	'vuejs-accessibility/no-onchange': 'warn',
}

export default [
	{
		ignores: [
			'dist/**',
			'node_modules/**',
			'playwright-report/**',
			'test-results/**',
			'reports/**',
		],
	},
	...vue.configs['flat/base'],
	{
		files: ['src/**/*.vue'],
		languageOptions: {
			parser: vueParser,
			parserOptions: {
				parser: tsParser,
				ecmaVersion: 'latest',
				sourceType: 'module',
				extraFileExtensions: ['.vue'],
			},
		},
		plugins: {
			'vuejs-accessibility': vueA11y,
		},
		rules: a11yRules,
	},
	{
		files: ['src/**/*.ts'],
		languageOptions: {
			parser: tsParser,
			ecmaVersion: 'latest',
			sourceType: 'module',
		},
	},
]
