package com.somepro.interfaces.rest.collateral;

import com.somepro.application.collateral.CollateralAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.collateral.converter.CollateralVoConverter;
import com.somepro.interfaces.rest.collateral.dto.CollateralCreateRequest;
import com.somepro.interfaces.rest.collateral.dto.CollateralIdRequest;
import com.somepro.interfaces.rest.collateral.dto.CollateralUpdateRequest;
import com.somepro.interfaces.rest.collateral.vo.CollateralVO;
import com.somepro.interfaces.rest.common.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 当物模块用户接口层：登记、修改、详情、退还、销掉、按条件翻清单。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link CollateralAppService}。
 * 入参统一用 @RequestParam / @ModelAttribute：表单 / query string / x-www-form-urlencoded 都能接，
 * 便于柜台端直接调用。
 */
@RestController
@RequestMapping("/api/collateral")
public class CollateralController {

    private final CollateralAppService collateralAppService;

    public CollateralController(CollateralAppService collateralAppService) {
        this.collateralAppService = collateralAppService;
    }

    /** 登记当物：状态固定 IN_STOCK，编号由服务端按 DW-年份-序号 生成；品相不填默认 GOOD。 */
    @PostMapping("/create")
    public Mono<Result<CollateralVO>> create(@ModelAttribute CollateralCreateRequest request) {
        return collateralAppService.register(request.getPawnerId(), request.getCategory(),
                        request.getItemName(), request.getBrand(),
                        request.getConditionLevel(), request.getAppraisedValue())
                .map(CollateralVoConverter::toVo)
                .map(Result::ok);
    }

    /** 修改：名称 / 品牌 / 品相随时能改；估值有变化时，被在当当票占着的由应用层挡回。 */
    @PostMapping("/update")
    public Mono<Result<CollateralVO>> update(@ModelAttribute CollateralUpdateRequest request) {
        return collateralAppService.update(request.getId(), request.getItemName(), request.getBrand(),
                        request.getConditionLevel(), request.getAppraisedValue())
                .map(CollateralVoConverter::toVo)
                .map(Result::ok);
    }

    /** 详情：id 或 itemNo 任一指定。 */
    @GetMapping("/detail")
    public Mono<Result<CollateralVO>> detail(@RequestParam(required = false) Long id,
                                             @RequestParam(required = false) String itemNo) {
        return collateralAppService.detail(id, itemNo)
                .map(CollateralVoConverter::toVo)
                .map(Result::ok);
    }

    /** 退还：只有在库的当物退得回去；被在当当票占着的由应用层挡回。办理时刻落在 updateTime。 */
    @PostMapping("/release")
    public Mono<Result<CollateralVO>> release(@ModelAttribute CollateralIdRequest request) {
        return collateralAppService.release(request.getId())
                .map(CollateralVoConverter::toVo)
                .map(Result::ok);
    }

    /** 销掉录错的当物：逻辑删除，清单里不再出现，账留着。 */
    @PostMapping("/delete")
    public Mono<Result<Void>> delete(@ModelAttribute CollateralIdRequest request) {
        return collateralAppService.delete(request.getId())
                .then(Mono.just(Result.ok()));
    }

    /**
     * 翻清单：当户 / 类别 / 品相 / 状态随意拼，都不填翻整份（已销掉的不出）。
     * pageNum/pageSize 由请求说了算，每行带 itemNo 便于与纸质登记本对号。
     */
    @GetMapping("/list")
    public Mono<Result<PageVO<CollateralVO>>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "20") int pageSize,
                                                   @RequestParam(required = false) Long pawnerId,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(required = false) String conditionLevel,
                                                   @RequestParam(required = false) String status) {
        return collateralAppService.page(pageNum, pageSize, pawnerId, category, conditionLevel, status)
                .map(CollateralVoConverter::toPageVo)
                .map(Result::ok);
    }
}
