# Week 4 — 트랜잭션·동시성 & 주문/쿠폰 구현 설계 QnA

> subject: `01-concurrency-notes.md` / quest: `02-implementation-quest.md`
> 현재 구현 상태: user·product 도메인만 존재. order·coupon·like·point 미구현.

---

## Q1. 어디서부터 구현할 것인가 — 구현 순서의 판단 기준

**[질문]**
"쿠폰 템플릿부터" vs "주문 뼈대부터" 두 후보가 있다. 구현 순서를 정할 때 가장 중요한 기준 하나를 꼽으라면 무엇인가? 그 기준으로 봤을 때 두 도메인은 서로 어떤 의존 관계를 갖는가? (주문을 먼저 짜려면 무엇이 이미 있어야 하고, 쿠폰은 무엇에 의존하는가?)

**[답변]**
> 쿠폰이 있어야지 주문도 온전히 동작하는지 확인할 수 있고, 그 대신 주문에 대한 직관적인 이해가 부족한 상태로 쿠폰을 만들 것 같아

**[후속 질문: couponId가 NULLABLE이라는 점 — 쿠폰은 주문의 "전제"인가 "옵션"인가]**
quest의 주문 요청 스펙에서 `couponId`는 NULLABLE(미적용 시 생략 가능)이다. 그렇다면 쿠폰 없이도 주문이 성립하는가? "쿠폰이 있어야 주문이 온전히 동작한다"는 말과, "couponId 없이도 주문은 동작한다(쿠폰은 할인 옵션일 뿐)"는 말 중 어느 쪽이 더 정확한가? 의존 방향(누가 누구를 필요로 하는가)을 다시 생각해보면?

**[답변]**
> B이고, 쿠폰 -> 주문

**[후속 질문: B와 "쿠폰→주문"의 모순? — 쿠폰의 두 얼굴(발급 vs 사용)]**
B를 골랐다 = "주문은 쿠폰 없이도 동작한다"(주문은 쿠폰에 의존하지 않음). 그런데 화살표는 "쿠폰 → 주문"(쿠폰이 주문에 의존?)이라고 했다. 이 둘은 충돌하는 것처럼 보인다. 충돌이 아니라면, 쿠폰의 어떤 부분은 주문이 없어도 되고(독립), 어떤 부분은 주문이 있어야만 동작하는가(의존)? 쿠폰을 기능별로 쪼개보면?

**[답변]**
> 1, 2번 발급/조회는 독립, 사용은 주문 의존

**[정리: 쿠폰의 두 얼굴]**
- 쿠폰 템플릿 / 발급 / 조회 → 독립 (주문 없이 만들고 테스트 가능)
- 쿠폰 사용(검증·할인·USED) → 주문 흐름 안에서만 동작 = 주문에 의존
- 따라서 "쿠폰 → 주문" 화살표는 정확히 **쿠폰 '사용'** 부분을 가리킨다. B(주문은 쿠폰 없이도 동작)와 모순 아님.

---

## Q2. 그래서 첫 단위는? — 의존 그래프 기반 빌드 순서

**[질문]**
독립적인 조각(leaf)을 먼저 만들고, 의존(통합)은 마지막에 붙이는 게 점진적 구현의 정석이다. 지금 의존 그래프는:
- 독립 leaf: ① 쿠폰 발급/조회, ② 주문 뼈대(product에만 의존, product는 이미 존재)
- 통합: ③ 주문 + 쿠폰 사용

처음 worry("주문 직관 없이 쿠폰부터 만든다")는 이 분해로 해소되는가? 둘 다 독립 leaf라면 어느 것을 1번으로 두겠는가? 그 근거는?

**[답변]**
> 쿠폰 발급 먼저 하자

**[결정]**
- 첫 단위 = ① 쿠폰 발급/조회 (독립 leaf, 기존 product CRUD 패턴과 유사해 워밍업)
- 구현은 계층별로 끊어서 진행: domain → repository/infra → service/facade → controller/dto
- 쿠폰 사용(할인·USED)·동시성은 ③ 통합 단계로 미룸

---

## Q3. (구현 중) 쿠폰 도메인 모델링

**[멘토 설명: 두 엔티티 분리]**
- `Coupon`(템플릿/정책): name, type(FIXED/RATE), value, minOrderAmount, expiredAt
- `UserCoupon`(발급된 쿠폰): userId, couponId(템플릿 참조), status
- 발급 = 템플릿을 보고 UserCoupon 인스턴스를 찍어내는 것

**[가정 — 사용자 확인 필요]**
- EXPIRED 상태는 **저장하지 않고** 조회 시점에 템플릿 `expiredAt`과 현재시각을 비교해 계산한다.
  → 저장 status는 AVAILABLE / USED만 가짐. (대안: 배치로 EXPIRED flip)

**[사용자 지적: 기존 코드 일관성 / 엔티티 null 검증]**
- 첫 구현이 `Product`(단순 스타일)만 따라해 `User`(리치 스타일)와 불일치.
- 이 코드베이스 검증 레이어링: VO record 생성자(의미 검증) + `@Column(nullable=false)`(DB 안전망). DTO엔 jakarta.validation 미사용. → **엔티티 생성자 null 재검증은 중복**.
- 정정 방향: coupon 엔티티를 User 리치 스타일(`@Getter`+`@NoArgsConstructor(PROTECTED)`+`@Column` 명시, 엔티티 null 체크 제거)로 맞춘다.

---

## Q4. 쿠폰의 비즈니스 규칙은 어디에 둘까 — VO 도입 여부

**[질문]**
User 패턴은 검증·불변식을 VO에 몰아넣는다(Email/Name/Birth...). 쿠폰의 진짜 비즈니스 규칙은 `value > 0`, `RATE일 때 value ≤ 100`이다. 이 규칙(type+value 묶음)을 별도 VO(예: `Discount`)로 뺄까, 아니면 엔티티에 둘까? VO로 빼면 나중에 할인 계산(`apply(orderAmount)`)도 거기 자연스럽게 들어간다. trade-off는?

**[답변]**
> A

**[결정 + 컨벤션 확인]**
- (A) `Discount` VO 도입. type+value를 묶어 검증(value>0, RATE≤100). 나중에 할인계산도 여기 추가.
- VO 생성 위치 = **Facade** (user 패턴: `new LoginId(...)`를 Facade에서 만들어 Service로 전달).
- 엔티티는 VO를 받아 **언래핑한 원시값을 @Column에 저장**.

---

## Q5. VO 아닌 필드의 null 검증 — 기존 코드 확인 후 하이브리드 결정

**[질문]** VO에서 null 검증하면, VO 아닌 필드(name·expiredAt)는 어디서 막나?

**[기존 코드 근거]**
- `User` = 전부 VO → 엔티티 null 체크 0개.
- `ProductModel` = VO 없음 → **엔티티 생성자에서 `CoreException(BAD_REQUEST)` 검증** (깔끔한 400, 추가 클래스 0).
- 즉 "VO 아닌 필드는 엔티티 생성자에서 검증"이 기존 정답. → 처음에 null 체크 전부 지운 건 과했음(User 전용 규칙을 오적용).

**[결정: 하이브리드]**
- `type·value`(불변식 있음) → `Discount` VO
- `name·expiredAt·minOrderAmount`(단순 존재/범위) → 엔티티 생성자에서 Product 스타일 검증
- `UserCoupon`의 userId·couponId도 동일하게 엔티티 생성자 검증

**[Discount VO 용도]** ① 생성 시 불변식 검증 ② `getDiscount()`로 재구성 ③ 3단계에서 `apply(주문금액)` 할인 계산 캡슐화(FIXED/RATE 분기). 지금은 ③ 미구현.

---

## Q6. ★중요★ 코드 재확인 — 첫 분석이 pull 이전 옛 코드였음

**[사용자 지적]** "pull 안 받고 코드 이해시켰다, 전부 다시 봐. VO 아닌 null들 어떻게 풀고 있나?"

**[재분석 결과]** 현재 워킹트리(volume-4)는 첫 분석과 완전히 다름:
- `ProductModel` → `Product` + `Stock`/`Money`/`Quantity` **@Embeddable VO**로 리팩터
- `order`(Order·OrderItem·OrderLine·OrderService·OrderFacade·Controller), `like`, `brand` **이미 구현 완료**
- 주문 트랜잭션 경계: `OrderFacade.place`(@Transactional) + `OrderService.place`(@Transactional) — **이미 Facade 경계 완성**

**[null 전략 = "출처 기반 선택적 검증"]**
- 사용자 입력 + 잘못될 수 있는 값 → VO 또는 entity `validate()` → 400
- 내부 보장값(인증 userId, 검증된 path id) → 명시적 체크 생략, `@Column(nullable=false)`만 (증거: `OrderItem` 생성자 검증 0개)
- 장점: 보일러플레이트 절감, VO 보장. 단점: 내부값이 null로 뚫리면 flush 시 500(늦은 실패). → 상류 보장에 베팅.
- 대안(전부 방어적 null 체크)은 코드베이스가 이미 거부(OrderItem) → 일관성 위해 비권장.

**[coupon 새 컨벤션 적용 — 최종]**
- `Discount` = **@Embeddable VO 클래스**(record 아님), `@Embedded`로 Coupon에 저장. type+value 불변식.
- `Coupon`: name·expiredAt → private static `validate()`(admin 입력 필수, 400). minOrderAmount → **Money VO**(nullable).
- `UserCoupon`: userId·couponId **null 체크 제거** → @Column에 위임(OrderItem과 동일).
- 컴파일 통과 확인.

---

## Q7. @Transactional을 Facade에 붙이는 기준 (week4 핵심)

**[질문]** CouponFacade.issue엔 @Transactional이 없고 OrderFacade.place엔 있다. 왜?

**[정리]**
- `@Transactional` = "다 같이 성공 or 다 같이 롤백"의 **작업 단위 경계**를 긋는 것.
- **판단 규칙**: Facade가 부르는 서비스가
  - **1개** → 경계는 그 서비스에 있음 → Facade @Transactional **불필요** (ProductFacade, CouponFacade.issue)
  - **여러 개를 원자적으로 묶음** → 경계는 **Facade**여야 함 → @Transactional **필수** (OrderFacade.place)
- **왜 필수인가**: 쿠폰 USED와 주문 저장이 각 서비스에 흩어져 있는데, Facade에 경계가 없으면 `couponService.use`가 혼자 커밋된 뒤 주문이 실패해도 쿠폰은 USED로 남음 → 원자성 깨짐.
- **어떻게 묶이나**: Facade @Transactional 안에서 부르는 서비스들의 @Transactional은 기본 전파 `REQUIRED`로 **기존 트랜잭션에 합류** → 물리적으로 트랜잭션 1개.
- → week4의 "OrderFacade 트랜잭션 경계 설계가 핵심"의 정체.

**핵심 한 줄**: 한 덩어리로 묶일 작업이 여러 서비스에 흩어져 있으면, 묶는 자리(Facade)에 경계를 긋는다.

---

## Q8. Application Service vs Facade vs Domain Service — 뭐가 다른가

**[질문]** 셋이 그냥 이름만 다른 건가?

**[정리]**
- **사실 2개**: "Application Service" == "Facade" (이 프로젝트가 application 레이어를 Facade로 명명). Domain Service는 별개.
- **Domain Service** (`domain/`, `OrderService`/`CouponService`): 비즈니스 **규칙**(HOW). 엔티티·레포지토리 다룸. 재고 차감, 쿠폰 검증 등. = 요리사.
- **Facade = App Service** (`application/`, `OrderFacade`): 유스케이스 **조율**(WHAT/순서) + 트랜잭션 경계 + DTO(Info) 변환. 규칙 자체는 모름. = 홀 매니저.
- **왜 둘 다(얇은 Facade라도)**: ① 여러 도메인 조율(Order+Coupon) ② 트랜잭션 경계 소유 ③ 도메인↔컨트롤러 차단(Info 변환). 조율이 커질 자리를 일관되게 마련.

**핵심 한 줄**: Domain Service=규칙 실행(요리사), Facade(=App Service)=유스케이스 조율(홀 매니저).

---

## Q9. 왜 CouponRepository / UserCouponRepository 둘로 나눴나

**[질문]** coupon 리포지토리를 user용과 따로 둔 이유? 파일이 여러 개인데.

**[정리]**
- DDD: **애그리거트 루트 하나당 리포지토리 하나.**
- 판단 테스트: 둘이 한 단위로 저장/조회되고 하나가 다른 하나 없이 존재 의미 없으면 → 한 애그리거트.
- `Coupon`(템플릿, admin 관리) vs `UserCoupon`(유저 소유, userId로 조회, status 독립 변경) → **서로 id 참조만 하는 독립 루트 둘** → 리포지토리 둘.
- **코드베이스 대조**:
  - `OrderItem` = Order의 **자식** → 자기 port 없음. OrderRepositoryImpl이 OrderJpa+OrderItemJpa 둘 다 써서 한 번에 저장.
  - `Like` = user·product를 id 참조하는 **독립 루트** → 자기 리포지토리. → `UserCoupon`은 Like와 동급.
- 리포지토리 1개 = 3파일(port + JpaRepository + Impl). 의존성 역전(도메인이 JPA에 직접 안 묶임).

**핵심 한 줄**: 애그리거트 루트 수 = 리포지토리 수. OrderItem(자식)은 흡수, UserCoupon(독립 루트)은 분리.

---

## Q10. 발급 응답 DTO 분리 (IssuedCouponInfo)

- 문제: 발급 Facade가 rich `UserCouponInfo`를 만들려고 템플릿을 재조회(double read). 발급 응답엔 템플릿 상세 불필요.
- 원인: 발급/조회가 rich DTO 하나를 공유 → 발급도 템플릿 필드를 채워야 함.
- 해결: `IssuedCouponInfo`(id·couponId·status, UserCoupon만으로 조립) 분리. 발급은 이걸 반환 → 재조회 0.
- `CouponService.getCoupon` 제거(이 변경으로 죽은 코드. order 통합 때 재추가 가능).
- 보충 개념: UserCoupon은 Coupon을 안 가짐, couponId(Long)만 가짐 → 상세 필요 시 findById로 복원(id 참조).

## (진행) 쿠폰 발급/조회 슬라이스 완료 (도메인→Repo→Service/Facade→API)

남은 작업: admin 템플릿 API / 주문+쿠폰 통합(할인·USED·트랜잭션) / 동시성 제어+테스트.

