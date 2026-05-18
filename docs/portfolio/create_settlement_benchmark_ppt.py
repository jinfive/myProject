from __future__ import annotations

import html
import os
import zipfile
from dataclasses import dataclass, field
from pathlib import Path


OUT = Path(__file__).with_name("settlement-benchmark-portfolio.pptx")
EMU = 914400
SLIDE_W = 13.333
SLIDE_H = 7.5


def emu(value: float) -> int:
    return int(value * EMU)


def esc(value: str) -> str:
    return html.escape(value, quote=True)


@dataclass
class Shape:
    kind: str
    x: float
    y: float
    w: float
    h: float
    text: str = ""
    fill: str = "FFFFFF"
    line: str = "D9E2EC"
    color: str = "0F172A"
    size: int = 18
    bold: bool = False
    align: str = "l"
    radius: bool = False
    items: list[str] = field(default_factory=list)


@dataclass
class Slide:
    title: str
    section: str
    message: str
    shapes: list[Shape] = field(default_factory=list)


def text_runs(text: str, size: int, color: str, bold: bool = False) -> str:
    lines = text.split("\n")
    parts = []
    for line in lines:
        parts.append(
            f"""
            <a:p>
              <a:r>
                <a:rPr lang="ko-KR" sz="{size * 100}" b="{1 if bold else 0}">
                  <a:solidFill><a:srgbClr val="{color}"/></a:solidFill>
                  <a:latin typeface="Aptos"/><a:ea typeface="Malgun Gothic"/>
                </a:rPr>
                <a:t>{esc(line)}</a:t>
              </a:r>
            </a:p>
            """
        )
    return "".join(parts)


def paragraph_text(shape: Shape) -> str:
    if shape.kind == "bullets":
        parts = []
        for item in shape.items:
            parts.append(
                f"""
                <a:p>
                  <a:pPr marL="285750" indent="-171450">
                    <a:buChar char="•"/>
                  </a:pPr>
                  <a:r>
                    <a:rPr lang="ko-KR" sz="{shape.size * 100}">
                      <a:solidFill><a:srgbClr val="{shape.color}"/></a:solidFill>
                      <a:latin typeface="Aptos"/><a:ea typeface="Malgun Gothic"/>
                    </a:rPr>
                    <a:t>{esc(item)}</a:t>
                  </a:r>
                </a:p>
                """
            )
        return "".join(parts)
    return text_runs(shape.text, shape.size, shape.color, shape.bold)


def shape_xml(shape: Shape, idx: int) -> str:
    geom = "roundRect" if shape.radius else "rect"
    fill = f"<a:solidFill><a:srgbClr val=\"{shape.fill}\"/></a:solidFill>" if shape.fill else "<a:noFill/>"
    line = f"<a:ln w=\"9525\"><a:solidFill><a:srgbClr val=\"{shape.line}\"/></a:solidFill></a:ln>" if shape.line else "<a:ln><a:noFill/></a:ln>"
    anchor = "ctr" if shape.align == "c" else "t"
    return f"""
    <p:sp>
      <p:nvSpPr>
        <p:cNvPr id="{idx}" name="Shape {idx}"/>
        <p:cNvSpPr txBox="1"/>
        <p:nvPr/>
      </p:nvSpPr>
      <p:spPr>
        <a:xfrm><a:off x="{emu(shape.x)}" y="{emu(shape.y)}"/><a:ext cx="{emu(shape.w)}" cy="{emu(shape.h)}"/></a:xfrm>
        <a:prstGeom prst="{geom}"><a:avLst/></a:prstGeom>
        {fill}
        {line}
      </p:spPr>
      <p:txBody>
        <a:bodyPr wrap="square" anchor="{anchor}" lIns="91440" tIns="68580" rIns="91440" bIns="68580"/>
        <a:lstStyle/>
        {paragraph_text(shape)}
      </p:txBody>
    </p:sp>
    """


def line_xml(x1: float, y1: float, x2: float, y2: float, idx: int, color: str = "2563EB") -> str:
    return f"""
    <p:cxnSp>
      <p:nvCxnSpPr>
        <p:cNvPr id="{idx}" name="Connector {idx}"/>
        <p:cNvCxnSpPr/>
        <p:nvPr/>
      </p:nvCxnSpPr>
      <p:spPr>
        <a:xfrm>
          <a:off x="{emu(min(x1, x2))}" y="{emu(min(y1, y2))}"/>
          <a:ext cx="{emu(abs(x2 - x1))}" cy="{emu(abs(y2 - y1))}"/>
        </a:xfrm>
        <a:prstGeom prst="straightConnector1"><a:avLst/></a:prstGeom>
        <a:ln w="25400">
          <a:solidFill><a:srgbClr val="{color}"/></a:solidFill>
          <a:tailEnd type="none"/>
          <a:headEnd type="triangle"/>
        </a:ln>
      </p:spPr>
    </p:cxnSp>
    """


def slide_xml(slide: Slide, page: int) -> str:
    shapes = [
        Shape("box", 0, 0, SLIDE_W, SLIDE_H, fill="F8FAFC", line="F8FAFC"),
        Shape("box", 0, 0, 0.18, SLIDE_H, fill="2563EB", line="2563EB"),
        Shape("text", 0.55, 0.32, 8.7, 0.45, slide.title, fill="", line="", color="0F172A", size=25, bold=True),
        Shape("text", 0.58, 0.86, 9.6, 0.3, slide.message, fill="", line="", color="2563EB", size=12, bold=True),
        *slide.shapes,
        Shape("text", 0.55, 7.05, 5.0, 0.25, slide.section, fill="", line="", color="64748B", size=9),
        Shape("text", 12.15, 7.05, 0.7, 0.25, f"{page:02d}", fill="", line="", color="64748B", size=9, align="c"),
    ]
    body = []
    for idx, shape in enumerate(shapes, start=2):
        body.append(shape_xml(shape, idx))
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
       xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr>
        <a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm>
      </p:grpSpPr>
      {''.join(body)}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""


def rels_xml(target: str, rel_type: str) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="{rel_type}" Target="{target}"/>
</Relationships>"""


def make_slides() -> list[Slide]:
    blue = "DBEAFE"
    light = "EFF6FF"
    green = "DCFCE7"
    amber = "FEF3C7"
    red = "FEE2E2"
    return [
        Slide(
            "정산 배치 성능 개선 프로젝트",
            "프로젝트 개요",
            "대용량 처리, 정합성, 멱등성, 실패 추적, 성능 개선을 함께 검증",
            [
                Shape("box", 0.75, 1.45, 2.25, 0.95, "Payment\n결제/취소 원천", blue, "BFDBFE", size=17, bold=True, align="c", radius=True),
                Shape("box", 3.75, 1.45, 2.25, 0.95, "Settlement\n가맹점별 정산", green, "BBF7D0", size=17, bold=True, align="c", radius=True),
                Shape("box", 6.75, 1.45, 2.55, 0.95, "BatchJobHistory\n실행 이력/실패 원인", amber, "FDE68A", size=16, bold=True, align="c", radius=True),
                Shape("box", 10.0, 1.45, 2.4, 0.95, "Merchant\n수수료율", light, "BFDBFE", size=17, bold=True, align="c", radius=True),
                Shape("bullets", 0.85, 3.05, 5.8, 1.7, items=["정산일자 기준 대량 Payment 집계", "전략별 Settlement 결과 비교", "성능 수치와 정합성 결과를 함께 기록"], fill="FFFFFF", line="E2E8F0", size=15, radius=True),
                Shape("bullets", 7.0, 3.05, 5.5, 1.7, items=["Spring Boot + JPA + PostgreSQL", "BatchJobHistory로 성공/실패 추적", "README/work-log/PPT용 기록 체계화"], fill="FFFFFF", line="E2E8F0", size=15, radius=True),
            ],
        ),
        Slide(
            "해결하려 한 문제",
            "문제 정의",
            "성능만 빠른 정산이 아니라, 틀리지 않고 추적 가능한 정산이 필요했다",
            [
                Shape("box", 0.65, 1.55, 2.25, 1.25, "전체 조회 비용\nPayment Entity 로딩 증가", "FFFFFF", "BFDBFE", size=15, bold=True, align="c", radius=True),
                Shape("box", 3.1, 1.55, 2.25, 1.25, "금액 정합성\n전략 변경 후 금액 불일치 위험", "FFFFFF", "BFDBFE", size=15, bold=True, align="c", radius=True),
                Shape("box", 5.55, 1.55, 2.25, 1.25, "중복 정산\n같은 날짜/전략 재실행 위험", "FFFFFF", "BFDBFE", size=15, bold=True, align="c", radius=True),
                Shape("box", 8.0, 1.55, 2.25, 1.25, "부분 저장\n중간 실패 시 일부 결과 저장", "FFFFFF", "BFDBFE", size=15, bold=True, align="c", radius=True),
                Shape("box", 10.45, 1.55, 2.25, 1.25, "저장 병목\nSettlement 10,000건 저장 비용", "FFFFFF", "BFDBFE", size=15, bold=True, align="c", radius=True),
                Shape("bullets", 1.05, 3.65, 5.35, 1.8, items=["배치가 실패해도 원인을 남겨야 함", "전략별 성능 비교가 가능해야 함", "금융 정산 결과는 속도보다 정합성이 우선"], fill=light, line="BFDBFE", size=16, radius=True),
                Shape("box", 6.8, 3.65, 5.35, 1.8, "핵심 관점\n문제 발견 → 원인 분석 → 대안 검토 → 수정 → 검증", "E0F2FE", "7DD3FC", color="0F172A", size=18, bold=True, align="c", radius=True),
            ],
        ),
        Slide(
            "정산 전략 비교",
            "전략 설계",
            "조회 병목과 저장 병목을 한 번에 섞지 않고 단계별로 분리",
            [
                Shape("box", 0.85, 1.55, 3.1, 1.25, "BASIC_LOOP\n전체 Payment 조회\nJava 반복 집계", "FFFFFF", "CBD5E1", size=16, bold=True, align="c", radius=True),
                Shape("box", 5.1, 1.55, 3.1, 1.25, "GROUP_BY_QUERY\nDB GROUP BY 집계\nEntity 조회 제거", "E0F2FE", "7DD3FC", size=16, bold=True, align="c", radius=True),
                Shape("box", 9.35, 1.55, 3.1, 1.25, "GROUP_BY_BULK_SAVE\n집계 결과 재사용\nsaveAll 저장 실험", "DCFCE7", "86EFAC", size=16, bold=True, align="c", radius=True),
                Shape("text", 4.2, 1.87, 0.55, 0.3, "→", fill="", line="", color="2563EB", size=28, bold=True, align="c"),
                Shape("text", 8.45, 1.87, 0.55, 0.3, "→", fill="", line="", color="2563EB", size=28, bold=True, align="c"),
                Shape("box", 0.95, 3.35, 3.0, 1.45, "목적\n성능 개선 전 기준선", "F8FAFC", "E2E8F0", size=15, align="c", radius=True),
                Shape("box", 5.15, 3.35, 3.0, 1.45, "목적\nPayment 전체 Entity 조회 제거", "F8FAFC", "E2E8F0", size=15, align="c", radius=True),
                Shape("box", 9.35, 3.35, 3.0, 1.45, "목적\n저장 방식 변화의 영향 확인", "F8FAFC", "E2E8F0", size=15, align="c", radius=True),
            ],
        ),
        Slide(
            "안정성/정합성 설계",
            "정합성/멱등성",
            "전략이 바뀌어도 결과는 같고, 실패는 추적 가능해야 한다",
            [
                Shape("box", 0.75, 1.45, 5.6, 1.25, "정상 흐름\nRUNNING → Settlement 저장 → SUCCESS", green, "86EFAC", size=18, bold=True, align="c", radius=True),
                Shape("box", 6.85, 1.45, 5.6, 1.25, "실패 흐름\nRUNNING → Settlement 롤백 → FAILED + errorMessage", red, "FCA5A5", size=18, bold=True, align="c", radius=True),
                Shape("bullets", 0.85, 3.2, 5.5, 2.2, items=["중복 기준: merchant_id + settlement_date + processing_strategy", "같은 날짜라도 다른 전략 결과는 저장 가능", "같은 날짜 + 같은 전략 재실행 차단"], fill="FFFFFF", line="E2E8F0", size=15, radius=True),
                Shape("bullets", 6.95, 3.2, 5.5, 2.2, items=["BatchJobHistory는 REQUIRES_NEW로 보존", "processedCount 확인", "Settlement 건수와 총액 동일성 검증"], fill="FFFFFF", line="E2E8F0", size=15, radius=True),
            ],
        ),
        Slide(
            "성능 개선 흐름",
            "개선 타임라인",
            "데이터 규모 확장 → 병목 확인 → 개선 방식 선택",
            [
                Shape("box", 0.55, 1.55, 1.8, 1.0, "10만 건\n기준선", "FFFFFF", "BFDBFE", size=14, bold=True, align="c", radius=True),
                Shape("box", 2.25, 2.75, 1.8, 1.0, "100만 건\n확장", "FFFFFF", "BFDBFE", size=14, bold=True, align="c", radius=True),
                Shape("box", 3.95, 1.55, 1.8, 1.0, "1000만 건\n단일 날짜", "FFFFFF", "BFDBFE", size=14, bold=True, align="c", radius=True),
                Shape("box", 5.65, 2.75, 1.8, 1.0, "work_mem\nspill 제거", "E0F2FE", "7DD3FC", size=14, bold=True, align="c", radius=True),
                Shape("box", 7.35, 1.55, 1.8, 1.0, "날짜 분산\n재설계", "FFFFFF", "BFDBFE", size=14, bold=True, align="c", radius=True),
                Shape("box", 9.05, 2.75, 1.8, 1.0, "인덱스\n실험", "E0F2FE", "7DD3FC", size=14, bold=True, align="c", radius=True),
                Shape("box", 10.75, 1.55, 1.8, 1.0, "native count(*)\nSEQUENCE", "DCFCE7", "86EFAC", size=14, bold=True, align="c", radius=True),
                Shape("bullets", 1.0, 4.55, 11.4, 1.15, items=["조회 병목: Entity 전체 조회 → DB GROUP BY → Index Only Scan", "메모리 병목: HashAggregate temp spill → 세션 work_mem 실험", "저장 병목: saveAll 한계 확인 → IDENTITY와 SEQUENCE 비교"], fill="FFFFFF", line="E2E8F0", size=15, radius=True),
            ],
        ),
        Slide(
            "트러블슈팅 1: work_mem과 temp spill",
            "메모리 병목",
            "병목을 감으로 판단하지 않고 EXPLAIN과 temp 지표로 확인",
            [
                Shape("box", 0.75, 1.35, 11.9, 0.45, "HashAggregate temp spill: 기본 4MB에서는 batches=5, temp read/write 발생", "FEF3C7", "FDE68A", size=16, bold=True, align="c", radius=True),
                Shape("box", 0.8, 2.05, 11.75, 2.15,
                      "work_mem     Execution Time     Batches     temp read     temp written\n"
                      "4MB          4,295.640ms        5           26,516        46,759\n"
                      "64MB         2,874.350ms        1           0             0\n"
                      "128MB        2,645.038ms        1           0             0\n"
                      "256MB        2,645.408ms        1           0             0",
                      "FFFFFF", "CBD5E1", size=14, radius=True),
                Shape("bullets", 1.0, 4.75, 5.6, 1.2, items=["64MB부터 temp spill 제거", "128MB와 256MB는 실행 시간 차이가 거의 없음"], fill=light, line="BFDBFE", size=15, radius=True),
                Shape("bullets", 6.95, 4.75, 5.2, 1.2, items=["전역 변경은 동시성 메모리 위험", "세션 단위 실험으로 판단 근거 확보"], fill=light, line="BFDBFE", size=15, radius=True),
            ],
        ),
        Slide(
            "트러블슈팅 2: JPQL count(p)와 Index Only Scan",
            "SQL 불일치",
            "Repository 코드가 아니라 실제 API SQL과 실행계획을 기준으로 판단",
            [
                Shape("box", 0.8, 1.45, 5.55, 1.25, "Before\nJPQL count(p) → SQL count(p.id)\ncovering index에 id 없음 → heap 접근", red, "FCA5A5", size=16, bold=True, align="c", radius=True),
                Shape("box", 6.95, 1.45, 5.55, 1.25, "After\nnative count(*) + interface projection\nIndex Only Scan, Heap Fetches=0", green, "86EFAC", size=16, bold=True, align="c", radius=True),
                Shape("bullets", 0.95, 3.25, 5.3, 1.65, items=["수동 SQL은 count(*)라 Index Only Scan", "API SQL은 count(p.id)로 heap 접근", "수동 SQL과 API SQL 실행계획이 달랐음"], fill="FFFFFF", line="E2E8F0", size=15, radius=True),
                Shape("bullets", 7.1, 3.25, 5.15, 1.65, items=["집계 쿼리를 native query로 분리", "count(*) 명시", "API 경로에서도 Index Only Scan 확인"], fill="FFFFFF", line="E2E8F0", size=15, radius=True),
                Shape("box", 2.0, 5.35, 9.4, 0.55, "EXPLAIN 2,242.501ms → 74.988ms / DB 집계조회 수백 ms대로 개선", "E0F2FE", "7DD3FC", size=17, bold=True, align="c", radius=True),
            ],
        ),
        Slide(
            "트러블슈팅 3: saveAll과 IDENTITY 한계",
            "저장 병목",
            "saveAll이라는 메서드명보다 SQL 로그와 ID 생성 전략을 확인",
            [
                Shape("box", 0.75, 1.35, 5.7, 1.25, "IDENTITY\ninsert 후 DB generated id 조회\nbatch insert에 불리", red, "FCA5A5", size=17, bold=True, align="c", radius=True),
                Shape("box", 6.85, 1.35, 5.7, 1.25, "SEQUENCE\ninsert 전 sequence로 id 확보\nbatch 처리에 유리", green, "86EFAC", size=17, bold=True, align="c", radius=True),
                Shape("box", 0.85, 3.0, 11.65, 2.1,
                      "전략                    ID 전략       저장 평균       API 전체 평균\n"
                      "GROUP_BY_QUERY          IDENTITY      1,105.225ms     1,490.271ms\n"
                      "GROUP_BY_BULK_SAVE      IDENTITY      975.471ms       1,289.364ms\n"
                      "GROUP_BY_QUERY          SEQUENCE      101.945ms       755.662ms\n"
                      "GROUP_BY_BULK_SAVE      SEQUENCE      40.449ms        696.352ms",
                      "FFFFFF", "CBD5E1", size=13, radius=True),
                Shape("box", 1.0, 5.65, 11.2, 0.45, "SQL 로그: saveAll 이후에도 개별 insert 반복 → Hibernate batch 설정과 ID 전략을 분리 실험", "E0F2FE", "7DD3FC", size=15, bold=True, align="c", radius=True),
            ],
        ),
        Slide(
            "최종 성능 결과와 배운 점",
            "결과 요약",
            "성능 개선 후에도 처리 건수, Settlement 건수, 총액 동일성을 확인",
            [
                Shape("box", 0.65, 1.25, 3.9, 1.55, "100만 건\nBASIC_LOOP 8,253ms\nGROUP_BY_QUERY 899ms\nGROUP_BY_BULK_SAVE 798ms", "FFFFFF", "BFDBFE", size=15, bold=True, align="c", radius=True),
                Shape("box", 4.75, 1.25, 3.9, 1.55, "날짜 분산 1000만 건\nEXPLAIN 2,242.501ms → 74.988ms\nAPI GQ 3,021.333ms → 1,627.667ms\nAPI GBS 2,478.333ms → 2,058.000ms", "FFFFFF", "BFDBFE", size=13, bold=True, align="c", radius=True),
                Shape("box", 8.85, 1.25, 3.9, 1.55, "SEQUENCE 변경 후\nGROUP_BY_QUERY 755.662ms\nGROUP_BY_BULK_SAVE 696.352ms", "FFFFFF", "BFDBFE", size=15, bold=True, align="c", radius=True),
                Shape("bullets", 0.85, 3.4, 5.7, 1.85, items=["EXPLAIN, SQL 로그, 구간별 측정으로 판단", "processedCount=322,581 확인", "Settlement 10,000건 확인"], fill=light, line="BFDBFE", size=15, radius=True),
                Shape("bullets", 6.85, 3.4, 5.55, 1.85, items=["결제금액/취소금액/수수료/최종 정산금액 동일", "중복 실행 차단과 실패 이력 보존", "성능과 정합성을 함께 검증"], fill=light, line="BFDBFE", size=15, radius=True),
            ],
        ),
    ]


def content_types(slide_count: int) -> str:
    overrides = "\n".join(
        f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>'
        for i in range(1, slide_count + 1)
    )
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
  {overrides}
</Types>"""


def presentation_xml(slide_count: int) -> str:
    ids = "\n".join(
        f'<p:sldId id="{256 + i}" r:id="rId{i}"/>' for i in range(1, slide_count + 1)
    )
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId{slide_count + 1}"/></p:sldMasterIdLst>
  <p:sldIdLst>{ids}</p:sldIdLst>
  <p:sldSz cx="{emu(SLIDE_W)}" cy="{emu(SLIDE_H)}" type="wide"/>
  <p:notesSz cx="6858000" cy="9144000"/>
  <p:defaultTextStyle/>
</p:presentation>"""


def presentation_rels(slide_count: int) -> str:
    rels = []
    for i in range(1, slide_count + 1):
        rels.append(
            f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>'
        )
    rels.append(
        f'<Relationship Id="rId{slide_count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>'
    )
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  {''.join(rels)}
</Relationships>"""


SLIDE_MASTER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
  <p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles>
</p:sldMaster>"""

SLIDE_LAYOUT = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
  <p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>"""

THEME = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Settlement Portfolio">
  <a:themeElements>
    <a:clrScheme name="Office">
      <a:dk1><a:srgbClr val="0F172A"/></a:dk1><a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>
      <a:dk2><a:srgbClr val="1E293B"/></a:dk2><a:lt2><a:srgbClr val="F8FAFC"/></a:lt2>
      <a:accent1><a:srgbClr val="2563EB"/></a:accent1><a:accent2><a:srgbClr val="0EA5E9"/></a:accent2>
      <a:accent3><a:srgbClr val="10B981"/></a:accent3><a:accent4><a:srgbClr val="F59E0B"/></a:accent4>
      <a:accent5><a:srgbClr val="64748B"/></a:accent5><a:accent6><a:srgbClr val="EF4444"/></a:accent6>
      <a:hlink><a:srgbClr val="2563EB"/></a:hlink><a:folHlink><a:srgbClr val="1D4ED8"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="Aptos"><a:majorFont><a:latin typeface="Aptos"/><a:ea typeface="Malgun Gothic"/></a:majorFont><a:minorFont><a:latin typeface="Aptos"/><a:ea typeface="Malgun Gothic"/></a:minorFont></a:fontScheme>
    <a:fmtScheme name="Default"><a:fillStyleLst/><a:lnStyleLst/><a:effectStyleLst/><a:bgFillStyleLst/></a:fmtScheme>
  </a:themeElements>
</a:theme>"""


def build() -> None:
    slides = make_slides()
    if OUT.exists():
        OUT.unlink()
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types(len(slides)))
        z.writestr("_rels/.rels", rels_xml("ppt/presentation.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"))
        z.writestr("ppt/presentation.xml", presentation_xml(len(slides)))
        z.writestr("ppt/_rels/presentation.xml.rels", presentation_rels(len(slides)))
        z.writestr("ppt/slideMasters/slideMaster1.xml", SLIDE_MASTER)
        z.writestr("ppt/slideMasters/_rels/slideMaster1.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/></Relationships>""")
        z.writestr("ppt/slideLayouts/slideLayout1.xml", SLIDE_LAYOUT)
        z.writestr("ppt/slideLayouts/_rels/slideLayout1.xml.rels", rels_xml("../slideMasters/slideMaster1.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster"))
        z.writestr("ppt/theme/theme1.xml", THEME)
        z.writestr("docProps/core.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>정산 배치 성능 개선 프로젝트</dc:title><dc:creator>Codex</dc:creator></cp:coreProperties>""")
        z.writestr("docProps/app.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>Codex</Application></Properties>""")
        for i, slide in enumerate(slides, start=1):
            z.writestr(f"ppt/slides/slide{i}.xml", slide_xml(slide, i))
            z.writestr(f"ppt/slides/_rels/slide{i}.xml.rels", rels_xml("../slideLayouts/slideLayout1.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout"))
    print(OUT)


if __name__ == "__main__":
    build()
