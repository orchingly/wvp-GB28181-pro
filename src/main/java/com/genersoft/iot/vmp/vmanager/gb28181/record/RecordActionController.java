package com.genersoft.iot.vmp.vmanager.gb28181.record;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.opencv.frame.bean.StreamDetectionItem;
import com.genersoft.iot.vmp.storager.dao.StreamDetectionMapper;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name  = "录像事件")

@RestController
@RequestMapping("/api/record/action")
public class RecordActionController {
    private final static Logger logger = LoggerFactory.getLogger(RecordActionController.class);

    @Autowired
    private StreamDetectionMapper actionMapper;

    @Operation(summary = "录像事件查询")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @Parameter(name = "startTime", description = "开始时间", required = true)
    @Parameter(name = "endTime", description = "结束时间", required = true)
    @GetMapping("/list")
    @ResponseBody
    public List<StreamDetectionItem> requestRecordAction(@RequestParam String app,
                                                         @RequestParam String stream,
                                                         @RequestParam String startTime,
                                                         @RequestParam String endTime){

        if (app == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "app不能为空");
        }
        if (stream == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "stream不能为空");
        }
        if (startTime == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "startTime不能为空");
        }
        if (endTime == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "endTime不能为空");
        }

        return actionMapper.queryStreamAction(app, stream, startTime, endTime);

    }
}
