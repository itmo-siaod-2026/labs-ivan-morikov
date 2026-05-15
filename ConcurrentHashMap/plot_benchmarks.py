import matplotlib.pyplot as plt
import numpy as np

threads = [1, 2, 4, 8]

# Данные из BenchmarkMultiThread (ops/ms)
get_custom  = [41684, 75977, 129317, 159092]
get_jdk     = [42286, 76113, 128480, 154446]

put_custom  = [16262, 13689, 13327, 13681]
put_jdk     = [12774, 33829, 55579, 80164]

# HashMap baseline (1 поток) — горизонтальная линия
get_hashmap = 43398
put_hashmap = 15938

fig, axes = plt.subplots(1, 2, figsize=(14, 6))

# --- GET ---
ax = axes[0]
ax.plot(threads, get_custom, 'o-', label='Custom ConcurrentHashMap', linewidth=2, markersize=8)
ax.plot(threads, get_jdk, 's-', label='JDK ConcurrentHashMap', linewidth=2, markersize=8)
ax.axhline(y=get_hashmap, color='gray', linestyle='--', label=f'HashMap (1 thread): {get_hashmap:,}')

# идеальное линейное масштабирование
# ideal = [get_custom[0] * t for t in threads]
# ax.plot(threads, ideal, ':', color='lightgray', label='Ideal linear scaling')

ax.set_xlabel('Threads', fontsize=12)
ax.set_ylabel('Throughput (ops/ms)', fontsize=12)
ax.set_title('GET: Throughput vs Threads', fontsize=14)
ax.set_xticks(threads)
ax.legend()
ax.grid(True, alpha=0.3)

# --- PUT ---
ax = axes[1]
ax.plot(threads, put_custom, 'o-', label='Custom ConcurrentHashMap', linewidth=2, markersize=8)
ax.plot(threads, put_jdk, 's-', label='JDK ConcurrentHashMap', linewidth=2, markersize=8)
ax.axhline(y=put_hashmap, color='gray', linestyle='--', label=f'HashMap (1 thread): {put_hashmap:,}')

# ideal_put = [put_jdk[0] * t for t in threads]
# ax.plot(threads, ideal_put, ':', color='lightgray', label='Ideal linear scaling')

ax.set_xlabel('Threads', fontsize=12)
ax.set_ylabel('Throughput (ops/ms)', fontsize=12)
ax.set_title('PUT: Throughput vs Threads', fontsize=14)
ax.set_xticks(threads)
ax.legend()
ax.grid(True, alpha=0.3)

plt.tight_layout()
plt.savefig('thread_scaling.png', dpi=150, bbox_inches='tight')
plt.show()
print("Saved: thread_scaling.png")
