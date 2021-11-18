#include<stdio.h>
#include<unistd.h>
#include<stdint.h>
#include<stdlib.h>

void _System_out_println(int32_t i) {
	printf("%i\n", i);
}

void _System_out_write(int32_t i) {
	write(STDOUT_FILENO, &i, 1);
}

void _System_out_flush() {
	fflush(stdout);
}

int32_t _System_in_read() {
	int32_t buf = 0;
	read(STDOUT_FILENO, &buf, 1);

	return buf;
}

void *__builtin_alloc_function__(uint32_t obj_size, uint32_t size) {
	return calloc(obj_size, size);
}
