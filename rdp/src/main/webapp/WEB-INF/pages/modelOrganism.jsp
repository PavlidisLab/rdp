<div class="container">
    <form id="modelOrganism" class="form-horizontal" role="form">

        <div class="form-group">
            <label class="col-sm-3 control-label">Organism</label>
            <div class="col-sm-6">
                <select class="form-control">
                    <option>Mouse</option>
                    <option>Rat</option>
                    <option>Fly</option>
                    <option>Worm</option>
                    <option>Monkey</option>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">About</label>
            <div class="col-sm-6">
                <textarea class="form-control" rows="3"
                    placeholder="My lab studies ..."></textarea>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">Bulk upload</label>
            <div class="col-sm-6">
                <input type="file" id="bulkUploadInputFile">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">Genes</label>
            <div class="col-sm-6">
                <input type="text" class="form-control" id="BRCA1"
                    value="BRCA1"> <input type="text"
                    class="form-control" id="APOE" value="APOE">

            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">Pathways /
                processes</label>
            <div class="col-sm-6">
                <input type="text" class="form-control"
                    id="Synapse Formation" value="Synapse Formation">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">Diseases /
                phenotypes</label>
            <div class="col-sm-6">
                <input type="text" class="form-control"
                    id="Microcephaly" value="Microcephaly"> <input
                    type="text" class="form-control" id="autism"
                    value="autism"> <input type="text"
                    class="form-control" id="cancer" value="cancer">
            </div>
        </div>

        <button type="submit" class="btn btn-default col-sm-offset-3">Save</button>
    </form>
</div>