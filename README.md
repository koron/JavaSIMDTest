# Java SIMD test

Java で簡単なベクトルの足し算に SSE が適用されるかどうか検証してみた。
~~結果、されてないっぽい。もしくは UseSSE が機能してないのか。~~
(JDK 1.7での結果)

Java 21 では UseSuperWord を使うことで
何かしらのSIMDの恩恵を受けていることがわかった。

## テスト内容

1000次元のベクトルを10万個作りランダムに初期化して1セットとする。
それを2セット用意する。
両セットの n 番目のベクトル同士を足し算するのを、5秒間繰り返す。
1秒あたりに足せた回数を指標とする。

そもそもテスト内容自体、メモリの読み書きの要素が強く計算の要素が弱い。
SIMDのパワーを見せるという観点では迫力に欠ける。
しかしJavaにおいてSIMDが機能していることは確認できる。

## OpenJDK 21

JDK 1.7のときとは実行しているPC構成が変わってることに留意が必要。

```console
$ java -version
openjdk version "21.0.2" 2024-01-16
OpenJDK Runtime Environment (build 21.0.2+13-58)
OpenJDK 64-Bit Server VM (build 21.0.2+13-58, mixed mode, sharing)

$ javac VectorAddBench.java

$ java VectorAddBench
Wait 5.0 seconds
1612397.000 times/sec

$ java -XX:-UseSuperWord VectorAddBench
Wait 5.0 seconds
1079658.250 times/sec

$ java -XX:+UseSuperWord VectorAddBench
Wait 5.0 seconds
1632889.625 times/sec
```

上に示した通り `-XX:-UseSuperWord` を付けて実行した際に約40%低下している。
これは SIMD を利用していないことにより引き起こされた速度低下だと考えられる。
即ちJava 21はデフォルトでSIMDを利用した最適化を行っている。

### Java 公式ドキュメントより引用

<https://docs.oracle.com/javase/jp/21/docs/specs/man/java.html>

> -XX:UseSSE=version
>    指定されたバージョンのSSE命令セットを使用できます。 デフォルトで、サポートされている最高バージョンの(x 86のみ)に設定されます。

> -XX:UseAVX=version
>    指定されたバージョンのAVX命令セットを使用できます。 デフォルトで、サポートされている最高バージョンの(x 86のみ)に設定されます。

> -XX:+UseSuperWord
>    スカラー操作のスーパーワード操作への変換を有効にします。 スーパーワードとは、ベクトル化最適化です。 このオプションはデフォルトでは有効になります。 スカラー操作のスーパーワード操作への変換を無効にするには、-XX:-UseSuperWordを指定します。

[Java 7](https://docs.oracle.com/javase/jp/7/technotes/tools/windows/java.html)のドキュメントにはUseSSEもUseSuperWordも存在しなかった。

[Java 8](https://docs.oracle.com/javase/jp/8/docs/technotes/tools/windows/java.html) のドキュメントには UseSuperWord が存在した。

## 距離関数を実装し比較してみた

L2、コサイン、内積(Dot-Product)の4つの距離関数を、
SIMDを使ってと使わないで実装して速度を比較する。

以下がその結果。
コサイン距離を除いてSIMDが2倍以上、3倍未満のパフォーマンスが出ている。
コサイン距離においてSIMDの伸びが悪い理由は不明。
しかし同時に3つの変数の集約を行っているので、
SIMDレジスタに収まっていない可能性が考えられる。

```console
$ make benchmark-distance
java --add-modules jdk.incubator.vector DistanceBenchmark
WARNING: Using incubator modules: jdk.incubator.vector
Type    Method  Dur(ms) Check
NORMAL  L2      3403    3.63823e+07
SIMD    L2      1408    3.63823e+07
NORMAL  COS     4147    1.50109e+06
SIMD    COS     3044    1.50109e+06
NORMAL  DP      3431    1.01050e+09
SIMD    DP      1260    1.01050e+09
```

実行時に `-XX:-UseSuperWord` を指定してSIMDを無効化して再計測。
ほぼSIMD有効化時と一緒なのだが非SIMD実装のコサイン距離が、
SIMD有効化時より約1割パフォーマンスが向上している。
原因は不明だが、SIMDとのやり取りにかかるオーバーヘッドがなくなったから、
という解釈は考えられる。

```console
$ make benchmark-distance-nosimd
java -XX:-UseSuperWord --add-modules jdk.incubator.vector DistanceBenchmark
WARNING: Using incubator modules: jdk.incubator.vector
Type    Method  Dur(ms) Check
NORMAL  L2      3531    3.63861e+07
SIMD    L2      1424    3.63861e+07
NORMAL  COS     3656    1.50122e+06
SIMD    COS     3030    1.50122e+06
NORMAL  DP      3510    1.01155e+09
SIMD    DP      1299    1.01155e+09
```

## JDK 1.7

### Usage

```console
$ java -version
java version "1.7.0_45"
Java(TM) SE Runtime Environment (build 1.7.0_45-b18)
Java HotSpot(TM) 64-Bit Server VM (build 24.45-b08, mixed mode)

$ java VectorAddBench
Wait 5.0 seconds
913063.813 times/sec

$ java -XX:UseSSE=0 VectorAddBench
Wait 5.0 seconds
940231.625 times/sec

$ java -XX:UseSSE=3 VectorAddBench
Wait 5.0 seconds
920144.375 times/sec
```

### Results

Codition   |1st        |2nd        |3rd        |4th        |5th
-----------|----------:|----------:|----------:|----------:|----------:
NONE       | 940111.813| 958073.625| 938013.000| 903621.813| 943931.000
XX:UseSSE=0| 924325.000| 973035.813| 917085.375| 906528.000| 978477.813
XX:UseSSE=1| 930232.375| 929316.625| 968240.813| 978787.813| 971030.625
XX:UseSSE=2| 912946.813| 967318.188| 944087.375| 970864.188| 971274.625
XX:UseSSE=3| 962921.188| 936130.188| 966030.000| 966046.375| 961669.625
