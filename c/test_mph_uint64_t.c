#include "spooky.h"
#include "mph.h"
#include <stdio.h>
#include <inttypes.h>
#include <fcntl.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <string.h>
#include <sys/time.h>
#include <sys/resource.h>

static uint64_t get_system_time(void) {
	struct timeval tv;
	gettimeofday(&tv, NULL);
	return tv.tv_sec * 1000000 + tv.tv_usec;
}

int main(int argc, char* argv[]) {
	int h = open(argv[1], O_RDONLY);
	assert(h >= 0);
	mph *mph = load_mph(h);
	close(h);

#define NKEYS 10000000
	h = open(argv[2], O_RDONLY);
	uint64_t *data = calloc(NKEYS, sizeof *data);
	read(h, data, NKEYS * sizeof *data);
	close(h);
	
	uint64_t total = 0;
	uint64_t u = 0;

	for(int k = 10; k-- != 0; ) {
		int64_t elapsed = - get_system_time();
		for (int i = 0; i < NKEYS; ++i) u ^= get_uint64_t(mph, data[i]);

		elapsed += get_system_time();
		total += elapsed;
		printf("Elapsed: %.3fs; %.3f ns/key\n", elapsed * 1E-6, elapsed * 1000. / NKEYS);
	}
	const volatile int unused = u;
	printf("\nAverage time: %.3fs; %.3f ns/key\n", (total * .1) * 1E-6, (total * .1) * 1000. / NKEYS);
}
