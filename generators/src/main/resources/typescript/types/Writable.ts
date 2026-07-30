// This set of types is inspired by https://stackoverflow.com/a/62362197/159522
// The final `Writable` utility type will create a deep copy of a type with all
// `readonly` properties removed.
//
// Providing "Writable" versions of our models is important because we know that
// POST operations do not accept readonly model properties.
type IfEquals<X, Y, A=X, B=never> =
	(<T>() => T extends X ? 1 : 2) extends
	(<T>() => T extends Y ? 1 : 2) ? A : B;

type WritableKeys<T> = {
	[P in keyof T]: IfEquals<{ [Q in P]: T[P] }, { -readonly [Q in P]: T[P] }, P>
}[keyof T];

// Map each attribute to itself or never, depending on whether it is optional.
// An empty Object extends a type containing only attribute P when P is
// optional ({} extends { a?: string } but not { a: string }).
type OptionalKeys<T> = {
	[P in keyof T]: {} extends Pick<T, P> ? P : never;
}[keyof T];

// If a key is not optional then it is required, so the required keys are all
// keys minus the optional ones.
type RequiredKeys<T> = Exclude<keyof T, OptionalKeys<T>>;

type WritablePrimitive = undefined | null | boolean | string | number | Function;

type WritableObject<T> = {
	// Capture required keys.
	[K in keyof T & RequiredKeys<T> & WritableKeys<T>]: Writable<T[K]>;
} & {
	// Capture optional keys, preserving optionality.
	[K in keyof T & OptionalKeys<T> & WritableKeys<T>]?: Writable<T[K]>;
} extends infer O
	? {
			// Flatten the required and optional types into a single type.
			[K in keyof O]: O[K];
		}
	: never;

export type Writable<T> =
	T extends WritablePrimitive ? T :
	T extends Array<infer U> ? Array<Writable<U>> :
	T extends Map<infer K, infer V> ? Map<K, Writable<V>> :
	T extends Set<infer X> ? Set<Writable<X>> :
	T extends Object ? WritableObject<T> :
	T;
