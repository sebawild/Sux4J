#include <inttypes.h>

typedef struct {
	uint64_t size;
	int chunk_shift;
	uint64_t global_seed;
	uint64_t edge_offset_and_seed_length;
	uint64_t *edge_offset_and_seed;
	uint64_t array_length;
	uint64_t *array;
} mph;

mph *load_mph(int h);
int64_t get_byte_array(const mph *mph, char *key, uint64_t len);
int64_t get_uint64_t(const mph *mph, uint64_t key);
